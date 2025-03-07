package no.nr.dbspec;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import no.nr.TreeSitterDbspec;
import org.apache.commons.text.StringEscapeUtils;

import no.nr.dbspec.RoaeMd.RoaeMdType;
import no.nr.dbspec.SiardMd.SiardMdType;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

import static no.nr.dbspec.Utils.getType;
import static no.nr.dbspec.Utils.ensureInstance;

@SuppressWarnings("SameParameterValue")
public class Interpreter {
    private final ScriptRunner scriptRunner;
    private final Path dir;
    private final Log log;
    private final Properties config;
    private final NormalContext context;
    private final Map<String, SiardMd> siardMd;
    private final Map<SiardMd, List<RoaeMd>> commandMds;
    private final Dbms dbms;
    private final SiardExtractor siardExtractor;
    private final SiardMetadataAdjuster siardMetadataAdjuster;
    private final RoaeProducer roaeProducer;

    private String source;
    private TSTree tree;

    public Interpreter(
            Log log,
            Path dir,
            ScriptRunner scriptRunner,
            Properties config,
            SiardExtractor siardExtractor,
            Dbms dbms,
            SiardMetadataAdjuster siardMetadataAdjuster,
            RoaeProducer roaeProducer) {
        this.scriptRunner = scriptRunner;
        this.context = new NormalContext();
        this.siardMd = new HashMap<>();
        this.commandMds = new HashMap<>();
        this.dbms = dbms;
        this.log = log;
        this.dir = dir;
        this.config = config;
        this.siardExtractor = siardExtractor;
        this.siardMetadataAdjuster = siardMetadataAdjuster;
        this.roaeProducer = roaeProducer;
    }

    void logNodeLines(TSNode n) {
        int start = n.getStartByte();
        while (start > 0 && source.charAt(start - 1) != '\n') {
            start--;
        }
        int end = n.getEndByte();
        int len = source.length();
        while (start < end && source.charAt(end - 1) == '\n') {
            end--;
        }
        while (end < len && source.charAt(end) != '\n') {
            end++;
        }
        int line = 0;
        int pos = -1;
        do {
            line++;
            pos = source.indexOf('\n', pos + 1);
        } while (pos < start && pos != -1);
        do {
            if (pos == -1 || pos > end) {
                pos = end;
            }
            log.error("%d:\t%s", line++, source.substring(start, pos));
            start = pos + 1;
            pos = source.indexOf('\n', start);
        } while (start < end);
    }

    public StatusCode interpret(Path file) {
        log.verbose("Parsing %s.", file);
        try {
            source = Files.readString(file);
        } catch (IOException e) {
            log.error("The file could not be read: %s\n%s", file, e.getMessage());
            return StatusCode.SPEC_UNREADABLE;
        }
        log.debug("--- Input ---\n%s-------------", source);
        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterDbspec());
        tree = parser.parseString(null, source);
        log.debug("AST: %s\n", (Supplier<String>) () -> tree.getRootNode().toString());

        log.verbose("Starting execution.");
        try {
            TSNode n = tree.getRootNode();
            interpretSourceFile(n, 0, context);
            return StatusCode.OK;
        } catch (SemanticFailure e) {
            log.error("Semantic error: %s", e.reason);
            logNodeLines(e.node);
            log.maybePrintStackTrace(e);
            return StatusCode.SEMANTIC_ERROR;
        } catch (SqlFailure e) {
            log.error("SQL error - %s", e.reason);
            logNodeLines(e.node);
            log.maybePrintStackTrace(e);
            return StatusCode.SQL_ERROR;
        } catch (ScriptError e) {
            log.error("Error in script - %s", e.reason);
            logNodeLines(e.node);
            log.maybePrintStackTrace(e);
            return StatusCode.SCRIPT_ERROR;
        } catch (AssertionFailure e) {
            log.error("Assertion failed");
            logNodeLines(e.node);
            for (Map.Entry<String, String> me : assertionCtx(e.node, e.context).entrySet()) {
                log.error("\t" + me.getKey() + "\t=\t" + me.getValue());
            }
            log.maybePrintStackTrace(e);
            return StatusCode.ASSERTION_FAILURE;
        } catch (AstFailure e) {
            log.error("Something went wrong when parsing.");
            logNodeLines(e.node);
            log.maybePrintStackTrace(e);
            return StatusCode.AST_ERROR;
        } catch (Exception e) {
            // Internal error. Log stack trace except with log level QUIET.
            log.printStackTrace(e);
            return StatusCode.INTERNAL_ERROR;
        }
    }

    // Methods corresponding to non-terminal AST nodes

    Stream<TSNode> getChildren(TSNode node) {
        // Does not work currently, since getNamedChild always returns the first named child:
        // return IntStream.range(0, node.getNamedChildCount()).mapToObj(node::getNamedChild);
        return IntStream.range(0, node.getChildCount()).mapToObj(node::getChild).filter(TSNode::isNamed);
    }

    void interpretSourceFile(TSNode n, int level, NormalContext ctx) {
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameters")) {
                interpretParameters(c, level + 1, ctx);
            } else {
                interpretStatement(c, level, ctx);
            }
        });
    }

    void interpretParameters(TSNode n, int level, NormalContext ctx) {
        log.debugIndented(level,"* Parameters");
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameter")) {
                interpretParameter(c, level + 1, ctx);
            } else {
                throw new AstFailure(n);
            }
        });
    }

    void interpretParameter(TSNode n, int level, NormalContext ctx) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        String descriptionString = interpretShortDescrOf(n, level, ctx);
        log.debugIndented(level, "* Parameter: %s  [%s]", nameString, descriptionString);
    }

    void interpretStatement(TSNode n, int level, NormalContext ctx) {
        if (n.getType().equals("nop")) {
            // NOP
        } else if (n.getType().equals("set")) {
            interpretSet(n, level + 1, ctx);
        } else if (n.getType().equals("execute_using")) {
            interpretExecuteUsing(n, level + 1, ctx);
        } else if (n.getType().equals("execute_sql")) {
            interpretExecuteSql(n, level + 1, ctx);
        } else if (n.getType().equals("siard_metadata")) {
            interpretSiardMetadata(n, level + 1, ctx);
        } else if (n.getType().equals("siard_output")) {
            interpretSiardOutput(n, level + 1, ctx);
        } else if (n.getType().equals("for_loop")) {
            interpretForLoop(n, level + 1, ctx);
        } else if (n.getType().equals("set_inter")) {
            // NOP
        } else if (n.getType().equals("log")) {
            interpretLog(n, level + 1, ctx);
        } else if (n.getType().equals("assert")) {
            interpretAssert(n, level + 1, ctx);
        } else if (n.getType().equals("conditional")) {
            interpretConditional(n, level + 1, ctx);
        } else {
            throw new AstFailure(n);
        }
    }

    void interpretLog(TSNode n, int level, Context ctx) {
        Object message = interpretBasicExpression(n.getNamedChild(0), level + 1, ctx);
        ensureInstance(n, "A log message ", message, String.class, BigInteger.class);
        log.debugIndented(level, "* Log message: '%s'", message);
        if (log.getLevel() > Log.QUIET) {
            System.out.println(Utils.prefixAndFixLineSeparators("", "" + message));
        }
    }

    // For printing if an assertion fails.
    private Map<String, String> assertionCtx(TSNode n, Context ctx) {
        return comparisonCtx(n.getNamedChild(0), ctx);
    }

    void interpretAssert(TSNode n, int level, Context ctx) {
        TSNode cn = n.getNamedChild(0);
        boolean comparisonValue = interpretComparison(cn, level, ctx);
        log.debugIndented(level, "* Assertion: '%s'", comparisonValue);
        if (!comparisonValue) {
            throw new AssertionFailure(n, ctx);
        }
    }

    void interpretSet(TSNode n, int level, NormalContext ctx) {
        TSNode name = n.getChildByFieldName("name");
        String variableName = interpretIdentifier(name, level + 1);
        TSNode value = n.getChildByFieldName("value");
        Object variableValue = value.getType().equals("raw") ? interpretRaw(value, level, ctx)
                                                             : interpretExpression(value, level, ctx);
        if (variableValue == null) {
            throw new AstFailure(n);
        }
        log.debugIndented(level, "* Set %s = '%s'", variableName, variableValue);
        ctx.setValue(variableName, variableValue);
    }

    Object interpretExpression(TSNode n, int level, Context ctx) {
        Object expressionValue = null;
        if (n.getType().equals("connection")) {
            expressionValue = interpretConnection(n, level + 1, ctx);
        } else if (n.getType().equals("query")) {
            expressionValue = interpretQuery(n, level + 1, ctx);
        } else if (n.getType().equals("script_result")) {
            expressionValue = interpretScriptResult(n, level + 1, ctx);
        } else {
            expressionValue = interpretBasicExpression(n, level, ctx);
        }
        return expressionValue;
    }

    void interpretExecuteUsing(TSNode n, int level, Context ctx) {
        TSNode interpreter = n.getChildByFieldName("interpreter");
        Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
        ensureInstance(n, "The interpreter command/path", interpreterString, String.class);
        TSNode script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        scriptRunner.execute(n, (String)interpreterString, scriptString, dir);
        log.debugIndented(level, "* Executing using interpreter '%s': '%s'", interpreterString, scriptString);
    }

    String interpretScriptResult(TSNode n, int level, Context ctx) {
        TSNode interpreter = n.getChildByFieldName("interpreter");
        Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
        ensureInstance(n, "The interpreter command/path", interpreterString, String.class);
        TSNode script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        log.debugIndented(level, "* Executing using interpreter %s: '%s'", interpreterString, scriptString);
        return scriptRunner.execute(n, (String)interpreterString, scriptString, dir);
    }

    Connection interpretConnection(TSNode n, int level, Context ctx) {
        TSNode url = n.getChildByFieldName("url");
        Object urlString = interpretBasicExpression(url, level, ctx);
        ensureInstance(n, "The URL", urlString, String.class);
        TSNode properties = n.getChildByFieldName("properties");
        log.debugIndented(level, "* Connection: %s", urlString);
        NormalContext connectionContext = interpretKeyValuePairs(properties, level + 1, ctx);
        try {
            return dbms.connect((String)urlString, connectionContext);
        } catch (SQLException e) {
            throw new SqlFailure(n, e.getMessage());
        }
    }

    void interpretExecuteSql(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object connectionObject = ctx.getValue(connectionString);
        ensureInstance(n, "The target", connectionObject, Connection.class);
        TSNode sql = n.getChildByFieldName("sql");
        Map.Entry<String, List<Object>> pair = interpretRawSql(sql, level + 1, ctx);
        log.debugIndented(level, "* Executing SQL via connection %s: '%s'", connectionString, pair.getKey());
        try {
            dbms.executeSqlUpdate((Connection)connectionObject, pair);
        } catch (SQLException e) {
            throw new SqlFailure(n, e.getMessage());
        }
    }

    Rows interpretQuery(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object connectionObject = ctx.getValue(connectionString);
        ensureInstance(n, "The target", connectionObject, Connection.class);
        TSNode sql = n.getChildByFieldName("sql");
        Map.Entry<String, List<Object>> pair = interpretRawSql(sql, level + 1, ctx);
        log.debugIndented(level, "* Executing SQL query via connection %s: '%s'", connectionString, pair.getKey());
        try {
            return new ResultSetRows(dbms.executeSqlQuery((Connection)connectionObject, pair));
        } catch (SQLException e) {
            throw new SqlFailure(n, e.getMessage());
        }
    }

    private String skipExtension(String filename) {
        Path p = Path.of(filename);
        Path f = p.getFileName();
        if (f == null) {
            return filename;
        }
        String n = f.toString();
        int i = n.lastIndexOf('.');
        return i == -1 ? filename : filename.substring(0, filename.length() - n.length() + i);
    }

    void interpretSiardOutput(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object connectionObject = ctx.getValue(connectionString);
        ensureInstance(n, "The source", connectionObject, Connection.class);
        Connection dbmsConnection = (Connection)connectionObject;
        Object file = interpretBasicExpression(n.getChildByFieldName("file"), level, ctx);
        ensureInstance(n, "The filename", file, String.class);
        String fileString = (String)file;
        log.debugIndented(level, "* SIARD output %s to '%s'", connectionString, fileString);
        Path path = dir.resolve(fileString);
        try {
            log.verbose("Creating/replacing %s.", path);
            siardExtractor.transfer(dbmsConnection, path);
        } catch (SiardException e) {
            String reason = "SIARD transfer failed";
            if (!e.getReason().isEmpty()) {
                reason += ": " + e.getReason();
            }
            // TODO: SemanticError does not seems entirely appropriate here.
            throw new SemanticFailure(n, reason);
        }
        SiardMd md = siardMd.get(connectionString);
        if (md == null || !md.hasChildren()) {
            log.debugIndented(level, "* No additional SIARD metadata.", md);
        } else {
            log.debugIndented(level, "* Additional SIARD metadata: %s", md);
            log.verbose("Adjusting metadata of %s.", path);
            try {
                siardMetadataAdjuster.updateMetadata(fileString, md, dbmsConnection);
            } catch (SiardException e) {
                // Maybe we should also delete the .siard file here to avoid confusion?
                String reason = "Adjusting SIARD metadata failed";
                if (!e.getReason().isEmpty()) {
                    reason += ": " + e.getReason();
                }
                throw new SemanticFailure(n, reason); // TODO: Same issue as above.
            }
        }
        String roaeFileString = skipExtension(fileString) + ".roae";
        try {
            Path roaePath = dir.resolve(roaeFileString);
            List<RoaeMd> commands = commandMds.get(md);
            if (commands == null || commands.isEmpty()) {
                String message = "No commands defined. ";
                if (Files.exists(roaePath)) {
                    Files.delete(roaePath);
                    message += "Deleting existing ROAE file: " + roaePath;
                } else {
                    message += "No ROAE file created.";
                }
                log.verbose(message);
                return;
            }
            log.verbose("Creating/replacing %s.", roaePath);
            roaeProducer.updateMetadata(roaePath, commands);
        } catch (IOException e) {
            // TODO: SemanticError does not seems entirely appropriate here either.
            throw new SemanticFailure(n, "Unable to write to " + roaeFileString + "\n" + e.getMessage());
        }
    }

    String interpretSiardMetadataField(String fieldName, TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode field = n.getChildByFieldName(fieldName);
        if (field.isNull()) {
            // Skip SIARD field when not provided
            // TODO: Should we make sure it is erased instead?
            return "";
        }
        Object fieldString = field.getType().equals("raw")
                ? interpretRaw(field, level, ctx)
                : interpretBasicExpression(field, level, ctx);
        ensureInstance(n, "The field '" + fieldName + "'", fieldString, String.class);
        log.debugIndented(level, "* SIARD metadata %s: %s", fieldName, fieldString);
        if (parent != null) {
            parent.add(new SiardMd(SiardMdType.INFO, fieldName, (String)fieldString));
        }
        return (String)fieldString;
    }

    void interpretSiardMetadata(TSNode n, int level, NormalContext ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        SiardMd md = siardMd.computeIfAbsent(connectionString, x ->
                new SiardMd(SiardMdType.METADATA, null, null));
        log.debugIndented(level, "* SIARD metadata for %s", connectionString);
        interpretSiardMetadataField("dbname", n, level + 1, ctx, md);
        interpretSiardMetadataField("description", n, level + 1, ctx, md);
        interpretSiardMetadataField("archiver", n, level + 1, ctx, md);
        interpretSiardMetadataField("archiverContact", n, level + 1, ctx, md);
        interpretSiardMetadataField("dataOwner", n, level + 1, ctx, md);
        interpretSiardMetadataField("dataOriginTimespan", n, level + 1, ctx, md);
        interpretSiardMetadataField("lobFolder", n, level + 1, ctx, md);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_schema")) {
                interpretSiardSchema(c, level + 1, ctx, md);
            } else if (c.getType().equals("command_declaration")) {
                interpretCommandDeclaration(c, level + 1, ctx, md);
            }
        });
    }

    private String interpretMdDescr(TSNode n, int level, Context ctx) {
        String sd = interpretShortDescrOf(n, level, ctx);
        return sd != null ? sd : interpretSiardMetadataField("description", n, level, ctx, null);
    }

    void interpretSiardSchema(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.SCHEMA, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD schema: %s [%s]", nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_type")) {
                interpretSiardType(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_table")) {
                interpretSiardTable(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_view")) {
                interpretSiardView(c, level + 1, ctx, md);
            }
        });
    }

    void interpretSiardType(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.TYPE, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD type: %s [%s]", nameString, descriptionString);
    }

    void interpretSiardTable(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.TABLE, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD table: %s [%s]", nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_column")) {
                interpretSiardColumn(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_key")) {
                interpretSiardKey(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_check")) {
                interpretSiardCheck(c, level + 1, ctx, md);
            }
        });
    }

    void interpretSiardColumn(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.COLUMN, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD column: %s [%s]", nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, md);
            }
        });
    }

    void interpretSiardField(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.FIELD, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD field: %s [%s]", nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, parent);
            }
        });
    }

    void interpretSiardKey(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.KEY, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD key: %s [%s]", nameString, descriptionString);
    }

    void interpretSiardCheck(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.CHECK, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD check: %s [%s]", nameString, descriptionString);
    }

    void interpretSiardView(TSNode n, int level, Context ctx, SiardMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretMdDescr(n, level, ctx);
        SiardMd md = new SiardMd(SiardMdType.VIEW, nameString, descriptionString);
        parent.add(md);
        log.debugIndented(level, "* SIARD view: %s [%s]", nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_column")) {
                interpretSiardColumn(c, level + 1, ctx, md);
            }
        });
    }

    void interpretCommandDeclaration(TSNode n, int level, NormalContext ctx, SiardMd parent) {
        TSNode title = n.getChildByFieldName("title");
        Object titleString = title.getType().equals("raw")
                ? interpretRaw(title, level, ctx)
                : interpretBasicExpression(title, level, ctx);
        ensureInstance(n, "The title", titleString, String.class);
        RoaeMd md = new RoaeMd(RoaeMdType.COMMAND, null, (String)titleString);
        commandMds.computeIfAbsent(parent, x -> new ArrayList<>()).add(md);
        log.debugIndented(level, "* Command declaration: %s", titleString);
        TSNode parameters = n.getChildByFieldName("parameters");
        if (!parameters.isNull()) {
            interpretCommandParameters(parameters, level + 1, ctx, md);
        }
        TSNode body = n.getChildByFieldName("body");

        Set<String> parameterSet = md
                .getChildren(RoaeMdType.PARAMETER)
                .stream()
                .map(MdBase::getName)
                .collect(Collectors.toSet());
        CommandContext mdCtx = new CommandContext(ctx, parameterSet);

        StringBuilder sb = new StringBuilder();
        interpretRawSegments(body,level + 1, mdCtx, sb::append, x ->
                sb.append("$${").append(Utils.escape(x, true)).append('}'));
        String bodyString = sb.toString();

        md.add(new RoaeMd(RoaeMdType.SQL, null, bodyString));
        log.debugIndented(level + 1, "%s", bodyString);
    }

    void interpretCommandParameters(TSNode n, int level, NormalContext ctx, RoaeMd parent) {
        log.debugIndented(level, "* Parameters");
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameter")) {
                interpretCommandParameter(c, level + 1, ctx, parent);
            } else {
                throw new AstFailure(n);
            }
        });
    }

    void interpretCommandParameter(TSNode n, int level, NormalContext ctx, RoaeMd parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        String descriptionString = interpretShortDescrOf(n, level, ctx);
        parent.add(new RoaeMd(RoaeMdType.PARAMETER, nameString, descriptionString));
        log.debugIndented(level, "* Parameter: %s  [%s]", nameString, descriptionString);
    }

    void interpretForLoop(TSNode n, int level, NormalContext ctx) {
        TSNode variables = n.getChildByFieldName("variables");
        List<String> variablesStrings = interpretForVariables(variables, level + 1);
        TSNode resultSet = n.getChildByFieldName("result_set");
        String resultSetString = interpretIdentifier(resultSet, level + 1);
        TSNode body = n.getChildByFieldName("body");
        log.debugIndented(level, "* For loop: %s in %s",
                String.join(", ", variablesStrings), resultSetString);
        Object resObj = ctx.getValue(resultSetString);
        ensureInstance(n, "The expression", resObj, Rows.class, String.class);
        Rows rs = asRows(n, resObj);
        int expectedCols = variablesStrings.size();
        try {
            if (!rs.tryLockAndRewind()) {
                throw new SemanticFailure(n, "Nested iteration over the same row set is not allowed.");
            }
            String[] row;
            while ((row = rs.next()) != null) {
                int actualCols = row.length;
                for (int i = 0; i < expectedCols; i++) {
                    String colValue = i < actualCols ? row[i] : null;
                    String var = variablesStrings.get(i);
                    if (colValue == null) {
                        ctx.clearValue(var);
                    } else {
                        ctx.setValue(var, colValue);
                    }
                }
                interpretStatementBlock(body, level + 1, ctx);
            }
        } catch (SQLException e) {
            throw new SqlFailure(n, "Problem iterating through the result set:\n" + e.getMessage());
        } finally {
            rs.free();
        }
    }

    List<String> interpretForVariables(TSNode n, int level) {
        List<String> variables = new ArrayList<String>();
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("identifier")) {
                variables.add(interpretIdentifier(c, level + 1));
            } else {
                throw new AstFailure(n);
            }
        });
        return variables;
    }

    void interpretConditional(TSNode n, int level, NormalContext ctx) {
        TSNode condition = n.getChildByFieldName("condition");
        Boolean comparisonValue = interpretComparison(condition, level + 1, ctx);
        log.debugIndented(level, "* Conditional: value = '%s'", comparisonValue);
        if (comparisonValue) {
            TSNode thenBlock = n.getChildByFieldName("then");
            interpretStatementBlock(thenBlock, level + 1, ctx);
        } else {
            TSNode elseBlock = n.getChildByFieldName("else");
            if (!elseBlock.isNull()) {
                interpretStatementBlock(elseBlock, level + 1, ctx);
            }
        }
    }

    void interpretStatementBlock(TSNode n, int level, NormalContext ctx) {
        log.debugIndented(level, "* Statement block");
        getChildren(n).forEach((TSNode c) -> {
            interpretStatement(c, level, ctx);
        });
    }

    // For printing if a comparison fails.
    Map<String, String> comparisonCtx(TSNode n, Context ctx) {
        return Stream.of("left", "right")
                .map(n::getChildByFieldName)
                .flatMap(this::basicExpressionVariableInstances)
                .distinct()
                .collect(Collectors.toMap(
                        var -> var,
                        var -> Utils.escape(ctx.getValue(var), true)));
    }

    Boolean interpretComparison(TSNode n, int level, Context ctx) {
        TSNode left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        TSNode operator = n.getChildByFieldName("operator");
        String operatorString = interpretComparisonOperator(operator, level + 1);
        TSNode right = n.getChildByFieldName("right");
        Object rightValue = interpretBasicExpression(right, level + 1, ctx);
        ensureInstance(n, "The left side of the comparison", leftValue, BigInteger.class, String.class, Rows.class);
        ensureInstance(n, "The right side of the comparison", rightValue, BigInteger.class, String.class, Rows.class);
        if (leftValue instanceof BigInteger && rightValue instanceof BigInteger) {
            int comparisonValue = ((BigInteger) leftValue).compareTo((BigInteger) rightValue);
            switch (operatorString) {
                case "==":
                    return comparisonValue == 0;
                case "!=":
                    return comparisonValue != 0;
                case "<":
                    return comparisonValue < 0;
                case ">":
                    return comparisonValue > 0;
                case "<=":
                    return comparisonValue < 1;
                case ">=":
                    return comparisonValue > -1;
                default:
                    throw new AstFailure(n);
            }
        } else if (leftValue instanceof String && rightValue instanceof String) {
            switch (operatorString) {
                case "==":
                    return leftValue.equals(rightValue);
                case "!=":
                    return !leftValue.equals(rightValue);
                case "<":
                case ">":
                case "<=":
                case ">=":
                    throw new SemanticFailure(n, "The only comparisons allowed between strings are '==' and '!='.");
                default:
                    throw new AstFailure(n);
            }
        } else if ((leftValue instanceof Rows || leftValue instanceof String)
                && (rightValue instanceof Rows || rightValue instanceof String)) { // Rows
            boolean eq;
            switch (operatorString) {
                case "==":
                    eq = true;
                    break;
                case "!=":
                    eq = false;
                    break;
                case "<":
                case ">":
                case "<=":
                case ">=":
                    throw new SemanticFailure(n, "The only comparisons allowed between lists of rows are '==' and '!='.");
                default:
                    throw new AstFailure(n);
            }
            Rows ls = asRows(n, leftValue);
            Rows rs = asRows(n, rightValue);
            if (ls == rs) {
                return eq;
            }
            try {
                if (!ls.tryLockAndRewind()) {
                    throw new SemanticFailure(n, "Already iterating over the left side.");
                }
                if (!rs.tryLockAndRewind()) {
                    throw new SemanticFailure(n, "Already iterating over the right side.");
                }
                String[] lr, rr;
                while ((lr = ls.next()) != null && (rr = rs.next()) != null) {
                    if (lr.length != rr.length) {
                        return !eq;
                    }
                    for (int i = 0; i < lr.length; i++) {
                        if (!Objects.equals(lr[i], rr[i])) {
                            return !eq;
                        }
                    }
                }
                return ((lr == null) && (rs.next() == null)) == eq;
            } catch (SQLException e) {
                throw new SqlFailure(n, "Problem checking if the result sets are identical:\n" + e.getMessage());
            } finally {
                ls.free();
                rs.free();
            }
        } else {
            throw new SemanticFailure(n, "Comparing expressions with incompatible types. Use .as_integer if necessary.");
        }
    }

    private Rows asRows(TSNode n, Object obj) {
        if (obj instanceof Rows) {
            return (Rows) obj;
        }
        if (obj instanceof String) {
            return new StringRows((String) obj);
        }
        throw new AstFailure(n);
    }

    // For printing if an assertion fails.
    private Stream<String> basicExpressionVariableInstances(TSNode n) {
        switch (n.getType()) {
            case "variable_instance":
                return Stream.of(interpretIdentifier(n.getNamedChild(0), -1));
            case "dot_expression":
                return basicExpressionVariableInstances(n.getChildByFieldName("left"));
            default:
                return Stream.empty();
        }
    }

    Object interpretBasicExpression(TSNode n, int level, Context ctx) {
        switch (n.getType()) {
            case "string":
                return interpretString(n, level + 1, ctx);
            case "variable_instance":
                return interpretVariableInstance(n, level + 1, ctx);
            case "integer":
                return interpretInteger(n, level + 1);
            case "dot_expression":
                return interpretDotExpression(n, level + 1, ctx);
            default:
                throw new AstFailure(n);
        }
    }

    Object interpretVariableInstance(TSNode n, int level, Context ctx) {
        TSNode identifier = n.getNamedChild(0);
        String identifierName = interpretIdentifier(identifier, level + 1);
        Object result = ctx.getValue(identifierName);
        if (result == null) {
            throw new SemanticFailure(n, "The variable '" + identifierName + "' has not been set.");
        }
        return result;
    }

    Object interpretDotExpression(TSNode n, int level, Context ctx) {
        TSNode left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        TSNode right = n.getChildByFieldName("right");
        String rightOperator = interpretDotOperator(right, level + 1);
        log.debugIndented(level, "* Dot expression: value = '%s'.'%s'", leftValue, rightOperator);
        return dotExpressionValue(n, leftValue, rightOperator);
    }

    private static Object dotExpressionValue(TSNode n, Object leftValue, String rightOperator) {
        if (leftValue instanceof String) {
            String trimmed = ((String) leftValue).trim();
            if (rightOperator.equals("stripped")) {
                return trimmed;
            } else if (rightOperator.equals("as_integer")) {
                try {
                    return new BigInteger(trimmed);
                } catch (NumberFormatException e) {
                    throw new SemanticFailure(n, "Not an integer: '" + leftValue + "'");
                }
            } else {
                throw new SemanticFailure(n, "Unsupported dot expression: " + rightOperator);
            }
        } else if (leftValue instanceof Rows) {
            Rows rs = (Rows) leftValue;
            switch (rightOperator) {
                case "size":
                    try {
                        return BigInteger.valueOf(rs.getSize());
                    } catch (SQLException e) {
                        throw new SqlFailure(n, "Problem finding the number of rows:\n" + e.getMessage());
                    }
                case "as_integer":
                case "stripped":
                    try {
                        if (!rs.tryLockAndRewind()) {
                            throw new SemanticFailure(n, "Nested iteration over the same row set is not allowed.");
                        }
                        String[] row = rs.next();
                        if (row == null) {
                            throw new SemanticFailure(n, "." + rightOperator + " applied to empty result set.");
                        }
                        if (rs.next() != null) {
                            throw new SemanticFailure(n, "." + rightOperator + " applied to result set with more than one row.");
                        }
                        if (row.length != 1) {
                            throw new SemanticFailure(n, "." + rightOperator + " applied to result set with " + row.length + " columns.");
                        }
                        return dotExpressionValue(n, row[0], rightOperator);
                    } catch (SQLException e) {
                        throw new SqlFailure(n, e.getMessage());
                    }
                default:
                    throw new SemanticFailure(n, "Unsupported dot expression: " + rightOperator);
            }
        } else {
            throw new SemanticFailure(n, "Illegal type in dot expression: " + getType(leftValue));
        }
    }

    String interpretString(TSNode n, int level, Context ctx) {
        return getChildren(n).map(c -> {
            switch (c.getType()) {
                case "interpolation":
                    String[] res = new String[1];
                    interpretInterpolation(c, level, ctx, s -> res[0] = s, i -> res[0] = i.toString(), null);
                    return res[0];
                case "escape_sequence":
                    return interpretEscapeSequence(c, level + 1);
                case "string_content":
                    return interpretStringContent(c, level + 1);
                default:
                    throw new AstFailure(n);
            }
        }).collect(Collectors.joining());
    }

    @SuppressWarnings("unused")
    String interpretShortDescrOf(TSNode n, int level, Context ctx) {
        TSNode description = n.getChildByFieldName("description");
        return !description.isNull() && description.getType().equals("short_description")
                ? interpretShortDescription(description, level + 1)
                : null;
    }

    void interpretInterpolation(
            TSNode n,
            int level,
            Context ctx,
            Consumer<String> stringArg,
            Consumer<BigInteger> intArg,
            Consumer<ParameterRef> parameterArg) {
        Object x = interpretBasicExpression(n.getNamedChild(0), level, ctx);
        if (x instanceof String) {
            stringArg.accept((String)x);
        } else if (x instanceof BigInteger) {
            intArg.accept((BigInteger)x);
        } else if (x instanceof ParameterRef) {
            parameterArg.accept((ParameterRef) x);
        } else {
            String message = "The interpolation expression must be a string"
                    + (parameterArg == null  ? " or an integer." : ", an integer or a parameter.");
            throw new SemanticFailure(n, message);
        }
    }

    NormalContext interpretKeyValuePairs(TSNode n, int level, Context ctx) {
        NormalContext keyValuePairs = new NormalContext(); // NB: does not inherit from ctx
        if (!n.isNull()) {
            getChildren(n).forEach(c -> {
                if (c.getType().equals("key_value_pair")) {
                    interpretKeyValuePair(c, level + 1, keyValuePairs, ctx);
                } else {
                    throw new AstFailure(n);
                }
            });
        }
        return keyValuePairs;
    }

    void interpretKeyValuePair(TSNode n, int level, NormalContext keyValuePairs, Context ctx) {
        TSNode key = n.getChildByFieldName("key");
        String keyString = interpretIdentifier(key, level + 1);
        TSNode value = n.getChildByFieldName("value");
        Object valueString = null;
        if (value.getType().equals("raw")) {
            valueString = interpretRaw(value, level, ctx);
        } else {
            valueString = interpretBasicExpression(value, level, ctx);
        }
        ensureInstance(n, "The " + keyString + " value", valueString, String.class);
        keyValuePairs.setValue(keyString, valueString);
    }

    String interpretRaw(TSNode n, int level, Context ctx) {
        StringBuilder sb = new StringBuilder();
        interpretRawSegments(n, level, ctx, sb::append, null);
        return sb.toString();
    }

    // We use Map.Entry since Java 11 has no built-in pair type
    Map.Entry<String, List<Object>> interpretRawSql(TSNode n, int level, Context ctx) {
        StringBuilder sb = new StringBuilder();
        List<Object> args = new ArrayList<>();
        interpretRawSegments(n, level, ctx, sb::append, x -> {
            sb.append('?');
            args.add(x);
        });
        return new AbstractMap.SimpleEntry<>(sb.toString(), args);
    }

    private void interpretRawSegments(
            TSNode n,
            int level,
            Context ctx,
            Consumer<String> literalConsumer,
            Consumer<Object> argumentConsumer) {
        StringBuilder pending = new StringBuilder();
        int nc = n.getChildCount();
        for (int i = 0; i < nc; i++) {
            TSNode c = n.getChild(i);
            if (!c.isNamed()) continue;
            if (c.getType().equals("raw_content")) {
                String rc = interpretRawContent(c, level + 1);
                // We trim trailing newlines stemming from newlines after raw.
                // This is an issue since we allow raw sections to continue after
                // non-indented empty lines.
                int len = rc.length();
                int end = len;
                while (end > 0 && rc.charAt(end - 1) == '\n') {
                    end--;
                    if (end > 0 && rc.charAt(end - 1) == '\r') {
                        end--;
                    }
                }
                if (end == 0) {
                    pending.append(rc);
                } else {
                    pending.append(rc, 0, end);
                    literalConsumer.accept(pending.toString());
                    pending.setLength(0);
                    pending.append(rc, end, len);
                }
                continue;
            }
            if (pending.length() > 0) {
                literalConsumer.accept(pending.toString());
                pending.setLength(0);
            }

            switch (c.getType()) {
                case "interpolation":
                    String[] val = new String[1];
                    interpretInterpolation(c, level, ctx,
                            s -> val[0] = s,
                            j -> val[0] = j.toString(),
                            p -> val[0] = "${" + p + "}");
                    literalConsumer.accept(val[0]);
                    break;
                case "interpolation2":
                    if (argumentConsumer == null) {
                        throw new SemanticFailure(c, "Safe interpolation ($$) cannot be used here.");
                    }
                    interpretInterpolation(c, level + 1, ctx,
                            argumentConsumer::accept,
                            argumentConsumer::accept,
                            argumentConsumer::accept);
                    break;
                default:
                    throw new AstFailure(c);
            }
        }
    }

    // Methods corresponding to terminal AST nodes

    @SuppressWarnings("unused")
    String interpretIdentifier(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    String interpretShortDescription(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    String interpretEscapeSequence(TSNode n, int level) {
        return StringEscapeUtils.unescapeJava(source.substring(n.getStartByte(), n.getEndByte()));
    }

    @SuppressWarnings("unused")
    String interpretStringContent(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    String interpretRawContent(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    String interpretComparisonOperator(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    String interpretDotOperator(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    @SuppressWarnings("unused")
    BigInteger interpretInteger(TSNode n, int level) {
        String content = source.substring(n.getStartByte(), n.getEndByte());
        return new BigInteger(content);
    }
}
