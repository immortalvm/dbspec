package no.nr.dbspec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import no.nr.TreeSitterDbspec;
import org.apache.commons.text.StringEscapeUtils;

import no.nr.dbspec.MdObject.MdType;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

public class Interpreter {
    String source;
    TSTree tree;
    Context context;
    Context connections;
    Dbms dbms;
    Siard siard;
    Log log;
    Properties config = new Properties();
    final static String CONFIG_FILENAME = "dbspec.conf";

    Interpreter(String filename, int verbosityLevel) {
        try {
            this.source = getDbSpecString(filename);
            this.context = new Context();
            this.connections = new Context();
            this.dbms = new Dbms();
            this.log = new Log(verbosityLevel);
            this.siard = new Siard(this.dbms, this.log);
            loadConfigFile(CONFIG_FILENAME);
            log.write(Log.DEBUG, "--- Input ---\n%s\n-------------\n", source);

            TSParser parser = new TSParser();
            parser.setLanguage(new TreeSitterDbspec());

            tree = parser.parseString(null, source);
            log.write(Log.DEBUG, "AST: %s\n\n", tree.getRootNode().toString());
        } catch (FileNotFoundException e) {
            log.write(Log.FATAL, "File not found\n");
        }
    }

    void loadConfigFile(String fileName) {
        try {
            config.load(new FileInputStream(fileName));
        } catch (Exception e) {
            log.write(Log.ERROR, "Unable to read configuration file '%s'\n", fileName);
        }
    }

    String getDbSpecString(String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner s = new Scanner(file).useDelimiter("\\Z");
        String result = s.next();
        s.close();
        return result;
    }

    void printNodeLines(TSNode n) {
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
            System.out.format("%d:\t%s\n", line++, source.substring(start, pos));
            start = pos + 1;
            pos = source.indexOf('\n', start);
        } while (start < end);
    }

    boolean interpret(int verbosityLevel) {
        log.setLevel(verbosityLevel);
        try {
            TSNode n = tree.getRootNode();
            log.write(Log.DEBUG, "Interpretation:\n\n");
            interpretSourceFile(n, 0, context);
            return true;
        } catch (AstError e) {
            System.out.format("AST error\n");
            printNodeLines(e.node);
            log.maybePrintStackTrace(e);
        } catch (SemanticError e) {
            System.out.format("Semantic error: %s\n", e.reason);
            printNodeLines(e.node);
            log.maybePrintStackTrace(e);
        } catch (SqlError e) {
            System.out.format("SQL error - %s\n", e.reason);
            printNodeLines(e.node);
            log.maybePrintStackTrace(e);
        } catch (ScriptError e) {
            System.out.format("Error in script - %s\n", e.reason);
            printNodeLines(e.node);
            log.maybePrintStackTrace(e);
        } catch (AssertionFailure e) {
            System.out.format("Assertion failed\n");
            printNodeLines(e.node);
            log.maybePrintStackTrace(e);
        } catch (Exception e) {
            log.printStackTrace(e);
        }
        return false;
    }

    String indent(int level) {
        String result = "";
        for (int i = 0; i < level; i++) {
            result += "  ";
        }
        return result;
    }

    // Methods corresponding to non-terminal AST nodes

    Stream<TSNode> getChildren(TSNode node) {
        // Does not work currently, since getNamedChild always returns the first named child:
        // return IntStream.range(0, node.getNamedChildCount()).mapToObj(node::getNamedChild);
        return IntStream.range(0, node.getChildCount()).mapToObj(node::getChild).filter(TSNode::isNamed);
    }

    void interpretSourceFile(TSNode n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* source file\n", indent(level));
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameters")) {
                interpretParameters(c, level + 1, ctx);
            } else {
                interpretStatement(c, level, ctx);
            }
        });
    }

    void interpretParameters(TSNode n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* parameters\n", indent(level));
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameter")) {
                interpretParameter(c, level + 1, ctx);
            } else {
                throw new AstError(n);
            }
        });
    }

    void interpretParameter(TSNode n, int level, Context ctx) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        TSNode description = n.getChildByFieldName("description");
        String descriptionString = "";
        if (!description.isNull() && description.getType().equals("short_description")) {
            descriptionString = interpretShortDescription(description, level + 1);
        }
        log.write(Log.DEBUG, "%s* parameter: %s  [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretStatement(TSNode n, int level, Context ctx) {
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
            throw new AstError(n);
        }
    }

    void interpretLog(TSNode n, int level, Context ctx) {
        Object logValue = interpretBasicExpression(n.getNamedChild(0), level + 1, ctx);
        log.write(Log.DEBUG, "%s* Log message: '%s'\n", indent(level), logValue);
        log.write(Log.INFO, "%s\n", logValue);
    }

    void interpretAssert(TSNode n, int level, Context ctx) {
        boolean comparisonValue = interpretComparison(n.getNamedChild(0), level, ctx);
        log.write(Log.DEBUG, "%s* Assertion: '%s'\n", indent(level), comparisonValue);
        if (!comparisonValue) {
            throw new AssertionFailure(n);
        }
    }

    void interpretSet(TSNode n, int level, Context ctx) {
        TSNode name = n.getChildByFieldName("name");
        String variableName = interpretIdentifier(name, level + 1);
        TSNode value = n.getChildByFieldName("value");
        Object variableValue = null;
        if (value.getType().equals("raw")) {
            variableValue = interpretRaw(value, level, ctx);
        } else {
            variableValue = interpretExpression(value, level, ctx);
        }
        if (variableValue == null) {
            throw new AstError(n);
        }
        log.write(Log.DEBUG, "%s* Set %s = '%s'\n", indent(level), variableName, variableValue);
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
        if (!(interpreterString instanceof String)) {
            throw new SemanticError(n, "Interpreter is not a string");
        }
        TSNode script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        Script.execute(n, (String)interpreterString, scriptString);
        log.write(Log.DEBUG, "%s* Executing using interpreter '%s': '%s'\n", indent(level), (String)interpreterString, scriptString);
    }

    String interpretScriptResult(TSNode n, int level, Context ctx) {
        TSNode interpreter = n.getChildByFieldName("interpreter");
        Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
        if (!(interpreterString instanceof String)) {
            throw new SemanticError(n, "Interpreter is not a string");
        }
        TSNode script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        String scriptResult = Script.execute(n, (String)interpreterString, scriptString);
        log.write(Log.DEBUG, "%s* Executing using interpreter %s: '%s'\n", indent(level), (String)interpreterString, scriptString);
        return scriptResult;
    }

    Connection interpretConnection(TSNode n, int level, Context ctx) {
        TSNode url = n.getChildByFieldName("url");
        Object urlString = interpretBasicExpression(url, level, ctx);
        if (!(urlString instanceof String)) {
            throw new SemanticError(n, "URL is not a string");
        }
        TSNode properties = n.getChildByFieldName("properties");
        log.write(Log.DEBUG, "%s* connection: %s\n", indent(level), (String)urlString);
        Context connectionContext = interpretKeyValuePairs(properties, level + 1, ctx);
        try {
            return dbms.connect((String)urlString, connectionContext);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    void interpretExecuteSql(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object dbmsConnection = ctx.getValue(connectionString);
        if (!(dbmsConnection instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        TSNode sql = n.getChildByFieldName("sql");
        String sqlString = interpretRaw(sql, level + 1, ctx);
        log.write(Log.DEBUG, "%s* Executing SQL on connection %s: '%s'\n", indent(level), connectionString, sqlString);
        try {
            dbms.executeSqlUpdate((Connection)dbmsConnection, sqlString);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    ResultSet interpretQuery(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object dbmsConnection = ctx.getValue(connectionString);
        if (!(dbmsConnection instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        TSNode sql = n.getChildByFieldName("sql");
        String sqlString = interpretRaw(sql, level + 1, ctx);
        log.write(Log.DEBUG, "%s* Executing SQL query on connection %s: '%s'\n", indent(level), connectionString, sqlString);
        try {
            return dbms.executeSqlQuery((Connection)dbmsConnection, sqlString);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    void interpretSiardOutput(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object connectionObject = ctx.getValue(connectionString);
        if (!(connectionObject instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        Connection dbmsConnection = (Connection)connectionObject;
        TSNode file = n.getChildByFieldName("file");
        Object fileString = interpretBasicExpression(file, level, ctx);
        if (!(fileString instanceof String)) {
            throw new SemanticError(n, "Filename is not a string");
        }
        String roaeFileString = ((String)fileString).indexOf('.') == -1 ? (String)fileString + ".roae" : ((String)fileString).replaceAll("\\.[^.]*$", ".roae");
        log.write(Log.DEBUG, "%s* SIARD output %s to '%s'\n", indent(level), connectionString, (String)fileString);
        try {
            siard.transfer(dbmsConnection, (String)fileString); // "lobs");
        } catch (SiardError e) {
            String reason = "SIARD transfer failed";
            if (!e.getReason().isEmpty()) {
                reason += ": " + e.getReason();
            }
            throw new SemanticError(n, reason);
        }
        MdObject md = (MdObject)connections.getValue(connectionString);
        if (md != null) {
            log.write(Log.DEBUG, "%s* SIARD metadata: %s\n", indent(level), md.toString());
            SiardMetadata.updateMetadata((String)fileString, md, dbmsConnection, n);
            log.write(Log.DEBUG, "%s* ROAE output to '%s'\n", indent(level), (String)roaeFileString);
            RoaeMetadata.updateMetadata(roaeFileString, md);
        } else {
            log.write(Log.WARNING, "%s* SIARD metadata not found\n", indent(level));
        }
    }

    String interpretSiardMetadataField(String fieldName, TSNode n, int level, Context ctx, MdObject parent) {
        TSNode field = n.getChildByFieldName(fieldName);
        if (field.isNull()) {
            // Skip SIARD field when not provided
            return "";
        }

        Object fieldString = null;
        if (field.getType().equals("raw")) {
            fieldString = interpretRaw(field, level, ctx);
        } else {
            fieldString = interpretBasicExpression(field, level, ctx);
        }
        if (!(fieldString instanceof String)) {
            throw new SemanticError(n, "Siard field " + fieldName + " is not a string");
        }
        log.write(Log.DEBUG, "%s* SIARD metadata %s: %s\n", indent(level), fieldName, (String)fieldString);
        MdObject md = new MdObject(MdType.INFO, fieldName, (String)fieldString);
        parent.add(md);
        return (String)fieldString;
    }

    void interpretSiardMetadata(TSNode n, int level, Context ctx) {
        TSNode connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        MdObject md = new MdObject(MdType.METADATA, "", "");
        connections.setValue(connectionString, md);
        log.write(Log.DEBUG, "%s* SIARD metadata for %s\n", indent(level), connectionString);
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

    void interpretSiardSchema(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.SCHEMA, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD schema: %s [%s]\n", indent(level), nameString, descriptionString);
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

    void interpretSiardType(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.TYPE, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD type: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardTable(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.TABLE, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD table: %s [%s]\n", indent(level), nameString, descriptionString);
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

    void interpretSiardColumn(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.COLUMN, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD column: %s [%s]\n", indent(level), nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, md);
            }
        });
    }

    void interpretSiardField(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.FIELD, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD field: %s [%s]\n", indent(level), nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, parent);
            }
        });
    }

    void interpretSiardKey(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.KEY, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD key: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardCheck(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.CHECK, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD check: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardView(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.VIEW, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD view: %s [%s]\n", indent(level), nameString, descriptionString);
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("siard_column")) {
                interpretSiardColumn(c, level + 1, ctx, md);
            }
        });
    }

    void interpretCommandDeclaration(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode title = n.getChildByFieldName("title");
        Object titleString = null;
        if (title.getType().equals("raw")) {
            titleString = interpretRaw(title, level, ctx);
        } else {
            titleString = interpretBasicExpression(title, level, ctx);
        }
        if (!(titleString instanceof String)) {
            throw new SemanticError(n, "Title is not a string");
        }
        MdObject md = new MdObject(MdType.COMMAND, "", (String)titleString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* Command declaration: %s\n", indent(level), (String)titleString);
        TSNode parameters = n.getChildByFieldName("parameters");
        interpretCommandParameters(parameters, level + 1, ctx, md);
        TSNode body = n.getChildByFieldName("body");
        String bodyString = interpretRaw(body, level + 1, new Context(ctx, true));
        MdObject mdSql= new MdObject(MdType.SQL, "", bodyString);
        md.add(mdSql);
        log.write(Log.DEBUG, "%s'%s'\n", indent(level), bodyString);
    }

    void interpretCommandParameters(TSNode n, int level, Context ctx, MdObject parent) {
        log.write(Log.DEBUG, "%s* parameters\n", indent(level));
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("parameter")) {
                interpretCommandParameter(c, level + 1, ctx, parent);
            } else {
                throw new AstError(n);
            }
        });
    }

    void interpretCommandParameter(TSNode n, int level, Context ctx, MdObject parent) {
        TSNode name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        TSNode description = n.getChildByFieldName("description");
        String descriptionString = "";
        if (!description.isNull() && description.getType().equals("short_description")) {
            descriptionString = interpretShortDescription(description, level + 1);
        }
        MdObject md = new MdObject(MdType.PARAMETER, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* parameter: %s  [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretForLoop(TSNode n, int level, Context ctx) {
        TSNode variables = n.getChildByFieldName("variables");
        List<String> variablesStrings = interpretForVariables(variables, level + 1);
        TSNode resultSet = n.getChildByFieldName("result_set");
        String resultSetString = interpretIdentifier(resultSet, level + 1);
        TSNode body = n.getChildByFieldName("body");
        log.write(Log.DEBUG, "%s* for_loop: %s in %s\n", indent(level),
                String.join(", ", variablesStrings), resultSetString);
        ResultSet rs = (ResultSet)(ctx.getValue(resultSetString));
        try {
            ResultSetMetaData rsmeta = rs.getMetaData();
            int ncols = Math.min(variablesStrings.size(), rsmeta.getColumnCount());
            rs.beforeFirst();
            while (rs.next()) {
                for (int i = 0; i < ncols; i++) {
                    String colValue = rs.getString(i + 1);
                    if (colValue == null) {
                        ctx.clearValue(variablesStrings.get(i));
                    } else {
                        ctx.setValue(variablesStrings.get(i), colValue);
                    }
                }
                interpretStatementBlock(body, level + 1, ctx);
            }
        } catch (SQLException e) {
            throw new SemanticError(n, "Error in ResultSet.");
        }
    }

    List<String> interpretForVariables(TSNode n, int level) {
        List<String> variables = new ArrayList<String>();
        getChildren(n).forEach((TSNode c) -> {
            if (c.getType().equals("identifier")) {
                variables.add(interpretIdentifier(c, level + 1));
            } else {
                throw new AstError(n);
            }
        });
        return variables;
    }

    void interpretConditional(TSNode n, int level, Context ctx) {
        TSNode condition = n.getChildByFieldName("condition");
        Boolean comparisonValue = interpretComparison(condition, level + 1, ctx);
        log.write(Log.DEBUG, "* Conditional: value = '%s'\n", comparisonValue);
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

    void interpretStatementBlock(TSNode n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* statement_block\n", indent(level));
        getChildren(n).forEach((TSNode c) -> {
            interpretStatement(c, level, ctx);
        });
    }

    Boolean interpretComparison(TSNode n, int level, Context ctx) {
        TSNode left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        TSNode operator = n.getChildByFieldName("operator");
        String operatorString = interpretComparisonOperator(operator, level + 1);
        TSNode right = n.getChildByFieldName("right");
        Object rightValue = interpretBasicExpression(right, level + 1, ctx);
        if (!(leftValue instanceof BigInteger)) {
            throw new SemanticError(n, "Left side of comparison is not integer");
        }
        if (!(rightValue instanceof BigInteger)) {
            throw new SemanticError(n, "Right side of comparison is not integer");
        }
        int comparisonValue = ((BigInteger)leftValue).compareTo((BigInteger)rightValue);
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
                throw new SemanticError(n, "Unknown comparison operator: " + operatorString);
        }
    }

    Object interpretBasicExpression(TSNode n, int level, Context ctx) {
        if (n.getType().equals("string")) {
            return interpretString(n, level + 1, ctx);
        } else if (n.getType().equals("variable_instance")) {
            return interpretVariableInstance(n, level + 1, ctx);
        } else if (n.getType().equals("integer")) {
            return interpretInteger(n, level + 1);
        } else if (n.getType().equals("dot_expression")) {
            return interpretDotExpression(n, level + 1, ctx);
        } else {
            throw new AstError(n);
        }
    }

    Object interpretVariableInstance(TSNode n, int level, Context ctx) {
        TSNode identifier = n.getNamedChild(0);
        String identifierName = interpretIdentifier(identifier, level + 1);
        Object result = ctx.getValue(identifierName);
        if (result == null) {
            throw new SemanticError(n, "The variable '" + identifierName + "' has not been set.");
        }
        return result;
    }

    Object interpretDotExpression(TSNode n, int level, Context ctx) {
        TSNode left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        TSNode right = n.getChildByFieldName("right");
        String rightOperator = interpretDotOperator(right, level + 1);
        log.write(Log.DEBUG, "* Dot expression: value = '%s'.'%s'\n", leftValue, rightOperator);
        if (leftValue instanceof String) {
            if (rightOperator.equals("stripped")) {
                return ((String)leftValue).trim();
            } else if (rightOperator.equals("as_integer")) {
                return new BigInteger((String)leftValue);
            } else {
                throw new SemanticError(n, "Unsupported dot expression");
            }
        } else if (leftValue instanceof ResultSet) {
            if (rightOperator.equals("size")) {
                ResultSet rs = (ResultSet)leftValue;
                try {
                    int currentRow = rs.getRow();
                    rs.last();
                    int lastRow = rs.getRow();
                    rs.absolute(currentRow);
                    return BigInteger.valueOf(lastRow);
                } catch (SQLException e) {
                    throw new SemanticError(n, "Error in ResultSet.");
                }
            } else {
                throw new SemanticError(n, "Unsupported dot expression");
            }
        } else {
            throw new SemanticError(n, "Illegal type in dot expression");
        }
    }

    String interpretString(TSNode n, int level, Context ctx) {
        return getChildren(n).map(c -> {
            if (c.getType().equals("interpolation")) {
                return interpretInterpolation(c, level + 1, ctx);
            } else if (c.getType().equals("escape_sequence")) {
                return interpretEscapeSequence(c, level + 1);
            } else if (c.getType().equals("string_content")) {
                return interpretStringContent(c, level + 1);
            } else {
                throw new AstError(n);
            }
        }).collect(Collectors.joining());
    }

    String interpretShortDescr(TSNode n, int level, Context ctx) {
        TSNode description = n.getChildByFieldName("description");
        return description.isNull() ? null : interpretShortDescription(description, level + 1);
    }

    String interpretInterpolation(TSNode n, int level, Context ctx) {
        return interpretInterpolationCommon(n, level, ctx);
    }

    String interpretInterpolation2(TSNode n, int level, Context ctx) {
        return interpretInterpolationCommon(n, level, ctx);
    }

    String interpretInterpolationCommon(TSNode n, int level, Context ctx) {
        Object interpolatedObject = interpretBasicExpression(n.getNamedChild(0), level, ctx);
        if (interpolatedObject instanceof String) {
            return (String)interpolatedObject;
        } else if (interpolatedObject instanceof BigInteger) {
            return String.valueOf((BigInteger)interpolatedObject);
        } else {
            throw new SemanticError(n, "Interpolation must be String or BigInteger");
        }
    }

    Context interpretKeyValuePairs(TSNode n, int level, Context ctx) {
        Context keyValuePairs = new Context(); // NB: does not inherit from ctx
        if (!n.isNull()) {
            getChildren(n).forEach(c -> {
                if (c.getType().equals("key_value_pair")) {
                    interpretKeyValuePair(c, level + 1, keyValuePairs, ctx);
                } else {
                    throw new AstError(n);
                }
            });
        }
        return keyValuePairs;
    }

    void interpretKeyValuePair(TSNode n, int level, Context keyValuePairs, Context ctx) {
        TSNode key = n.getChildByFieldName("key");
        String keyString = interpretIdentifier(key, level + 1);
        TSNode value = n.getChildByFieldName("value");
        Object valueString = null;
        if (value.getType().equals("raw")) {
            valueString = interpretRaw(value, level, ctx);
        } else {
            valueString = interpretBasicExpression(value, level, ctx);
        }
        if (!(valueString instanceof String)) {
            throw new SemanticError(n, "Value is not a string");
        }
        keyValuePairs.setValue(keyString, (String)valueString);
    }

    String interpretRaw(TSNode n, int level, Context ctx) {
        StringBuilder sb = new StringBuilder();
        int trailingNewlines = 0;
        int nc = n.getChildCount();
        for (int i = 0; i < nc; i++) {
            TSNode c = n.getChild(i);
            if (!c.isNamed()) continue;
            if (c.getType().equals("raw_content")) {
                String rc = interpretRawContent(c, level + 1);
                sb.append(rc);
                int len = rc.length();
                int end = len;
                while (end > 0 && rc.charAt(end - 1) == '\n') {
                    end--;
                }
                trailingNewlines = end == 0 ? trailingNewlines + len : len - end;
                continue;
            }
            trailingNewlines = 0;
            if (c.getType().equals("interpolation")) {
                sb.append(interpretInterpolation(c, level + 1, ctx));
            } else if (c.getType().equals("interpolation2")) {
                sb.append(interpretInterpolation2(c, level + 1, ctx));
            } else {
                throw new AstError(n);
            }
        }
        // Trim trailing newlines stemming from newlines after raw.
        // This is an issue since we allow raw sections to continue after
        // non-indented empty lines.
        return sb.substring(0, sb.length() - trailingNewlines);
    }

    // Methods corresponding to terminal AST nodes

    String interpretIdentifier(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretShortDescription(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretEscapeSequence(TSNode n, int level) {
        return StringEscapeUtils.unescapeJava(source.substring(n.getStartByte(), n.getEndByte()));
    }

    String interpretStringContent(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretRawContent(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretComparisonOperator(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretDotOperator(TSNode n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    BigInteger interpretInteger(TSNode n, int level) {
        String content = source.substring(n.getStartByte(), n.getEndByte());
        return new BigInteger(content);
    }
}
