package no.nr.dbspec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

import no.nr.dbspec.MdObject.MdType;

public class Interpreter {
    String source;
    Tree tree;
    Context context;
    Context connections;
    Dbms dbms;
    Siard siard;
    File jarDir;
    Log log;
    Properties config = new Properties();
    final static String CONFIG_FILENAME = "dbspec.conf";
    final static String TREE_SITTER_LIBRARY = "libjava-tree-sitter.so";

    Interpreter(String filename, int verbosityLevel) {
        try {
            this.jarDir = getJarDir();
            this.source = getDbSpecString(filename);
            this.context = new Context();
            this.connections = new Context();
            this.dbms = new Dbms();
            this.log = new Log(verbosityLevel);
            this.siard = new Siard(this.dbms, this.jarDir, this.log);
            File treesitterLibFile = new File(jarDir, TREE_SITTER_LIBRARY);
            System.load(treesitterLibFile.getAbsolutePath());
            loadConfigFile();
            log.write(Log.DEBUG, "--- JAR path: %s\n\n", jarDir);
            log.write(Log.DEBUG, "--- Input ---\n%s\n-------------\n", source);
            Parser parser = new Parser();
            parser.setLanguage(Languages.dbspec());
            tree = parser.parseString(source);
            log.write(Log.DEBUG, "AST: %s\n\n", tree.getRootNode().getNodeString());
            parser.close();
        } catch (FileNotFoundException e) {
            log.write(Log.FATAL, "File not found\n");
        } catch (UnsupportedEncodingException e) {
            log.write(Log.FATAL, "Unsupported encoding\n");
        } catch (JarPathError e) {
            log.write(Log.FATAL, "Could not find path of JAR file\n");
        }
    }

    void loadConfigFile() {
        try {
            config.load(new FileInputStream(CONFIG_FILENAME));
        } catch (Exception e) {
            log.write(Log.ERROR, "Unable to read configuration file '%s'\n", CONFIG_FILENAME);
        }
    }

    File getJarDir() throws JarPathError {
        try {
            String thisClassesResourcePath = Interpreter.class.getName().replace('.', '/') + ".class";
            String path = ClassLoader.getSystemResource(thisClassesResourcePath).getPath();
            File file = new File(new URI(path.substring(0, path.lastIndexOf("jar!") + 3)));
            return file.getParentFile();
        } catch (Exception e) {
            throw new JarPathError();
        }
    }

    String getDbSpecString(String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner s = new Scanner(file).useDelimiter("\\Z");
        String result = s.next();
        s.close();
        return result;
    }

    void printNodeLines(Node n) {
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
            Node n = tree.getRootNode();
            log.write(Log.DEBUG, "Interpretation:\n\n");
            interpretSourceFile(n, 0, context);
            return true;
        } catch (AstError e) {
            System.out.format("AST error\n");
            printNodeLines(e.node);
            if (log.getLevel() >= Log.DEBUG) {
                e.printStackTrace();
            }
        } catch (SemanticError e) {
            System.out.format("Semantic error: %s\n", e.reason);
            printNodeLines(e.node);
            if (log.getLevel() >= Log.DEBUG) {
                e.printStackTrace();
            }
        } catch (SqlError e) {
            System.out.format("SQL error - %s\n", e.reason);
            printNodeLines(e.node);
            if (log.getLevel() >= Log.DEBUG) {
                e.printStackTrace();
            }
        } catch (ScriptError e) {
            System.out.format("Error in script - %s\n", e.reason);
            printNodeLines(e.node);
            if (log.getLevel() >= Log.DEBUG) {
                e.printStackTrace();
            }
        } catch (AssertionFailure e) {
            System.out.format("Assertion failed\n");
            printNodeLines(e.node);
            if (log.getLevel() >= Log.DEBUG) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    void interpretSourceFile(Node n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* source file\n", indent(level));
        for (Node c : n.getChildren()) {
            if (c.getType().equals("parameters")) {
                interpretParameters(c, level + 1, ctx);
            } else {
                interpretStatement(c, level, ctx);
            }
        }
    }

    void interpretParameters(Node n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* parameters\n", indent(level));
        for (Node c : n.getChildren()) {
            if (c.getType().equals("parameter")) {
                interpretParameter(c, level + 1, ctx);
            } else {
                throw new AstError(n);
            }
        }
    }

    void interpretParameter(Node n, int level, Context ctx) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        Node description = n.getChildByFieldName("description");
        String descriptionString = "";
        if (description != null && description.getType().equals("short_description")) {
            descriptionString = interpretShortDescription(description, level + 1);
        }
        log.write(Log.DEBUG, "%s* parameter: %s  [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretStatement(Node n, int level, Context ctx) {
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

    void interpretLog(Node n, int level, Context ctx) {
        Object logValue = interpretBasicExpression(n.getChild(0), level + 1, ctx);
        log.write(Log.DEBUG, "%s* Log message: '%s'\n", indent(level), logValue);
        log.write(Log.INFO, "%s\n", logValue);
    }

    void interpretAssert(Node n, int level, Context ctx) {
        boolean comparisonValue = interpretComparison(n.getChild(0), level, ctx);
        log.write(Log.DEBUG, "%s* Assertion: '%s'\n", indent(level), comparisonValue);
        if (!comparisonValue) {
            throw new AssertionFailure(n);
        }
    }

    void interpretSet(Node n, int level, Context ctx) {
        Node name = n.getChildByFieldName("name");
        String variableName = interpretIdentifier(name, level + 1);
        Node value = n.getChildByFieldName("value");
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

    Object interpretExpression(Node n, int level, Context ctx) {
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

    void interpretExecuteUsing(Node n, int level, Context ctx) {
        Node interpreter = n.getChildByFieldName("interpreter");
        Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
        if (!(interpreterString instanceof String)) {
            throw new SemanticError(n, "Interpreter is not a string");
        }
        Node script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        Script.execute(n, (String)interpreterString, scriptString);
        log.write(Log.DEBUG, "%s* Executing using interpreter '%s': '%s'\n", indent(level), (String)interpreterString, scriptString);
    }

    String interpretScriptResult(Node n, int level, Context ctx) {
        Node interpreter = n.getChildByFieldName("interpreter");
        Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
        if (!(interpreterString instanceof String)) {
            throw new SemanticError(n, "Interpreter is not a string");
        }
        Node script = n.getChildByFieldName("script");
        String scriptString = interpretRaw(script, level + 1, ctx);
        String scriptResult = Script.execute(n, (String)interpreterString, scriptString);
        log.write(Log.DEBUG, "%s* Executing using interpreter %s: '%s'\n", indent(level), (String)interpreterString, scriptString);
        return scriptResult;
    }

    Connection interpretConnection(Node n, int level, Context ctx) {
        Node url = n.getChildByFieldName("url");
        Object urlString = interpretBasicExpression(url, level, ctx);
        if (!(urlString instanceof String)) {
            throw new SemanticError(n, "URL is not a string");
        }
        Node properties = n.getChildByFieldName("properties");
        log.write(Log.DEBUG, "%s* connection: %s\n", indent(level), (String)urlString);
        Context connectionContext = interpretKeyValuePairs(properties, level + 1, ctx);
        try {
            return dbms.connect((String)urlString, connectionContext);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    void interpretExecuteSql(Node n, int level, Context ctx) {
        Node connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object dbmsConnection = ctx.getValue(connectionString);
        if (!(dbmsConnection instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        Node sql = n.getChildByFieldName("sql");
        String sqlString = interpretRaw(sql, level + 1, ctx);
        log.write(Log.DEBUG, "%s* Executing SQL on connection %s: '%s'\n", indent(level), connectionString, sqlString);
        try {
            dbms.executeSqlUpdate((Connection)dbmsConnection, sqlString);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    ResultSet interpretQuery(Node n, int level, Context ctx) {
        Node connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object dbmsConnection = ctx.getValue(connectionString);
        if (!(dbmsConnection instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        Node sql = n.getChildByFieldName("sql");
        String sqlString = interpretRaw(sql, level + 1, ctx);
        log.write(Log.DEBUG, "%s* Executing SQL query on connection %s: '%s'\n", indent(level), connectionString, sqlString);
        try {
            return dbms.executeSqlQuery((Connection)dbmsConnection, sqlString);
        } catch (SQLException e) {
            throw new SqlError(n, e.getMessage());
        }
    }

    void interpretSiardOutput(Node n, int level, Context ctx) {
        Node connection = n.getChildByFieldName("connection");
        String connectionString = interpretIdentifier(connection, level + 1);
        Object dbmsConnection = ctx.getValue(connectionString);
        if (!(dbmsConnection instanceof Connection)) {
            throw new SemanticError(n, "Connection variable does not refer to an SQL connection");
        }
        Node file = n.getChildByFieldName("file");
        Object fileString = interpretBasicExpression(file, level, ctx);
        if (!(fileString instanceof String)) {
            throw new SemanticError(n, "Filename is not a string");
        }
        String roaeFileString = ((String)fileString).indexOf('.') == -1 ? (String)fileString + ".roae" : ((String)fileString).replaceAll("\\.[^.]*$", ".roae");
        log.write(Log.DEBUG, "%s* SIARD output %s to '%s'\n", indent(level), connectionString, (String)fileString);
        try {
            siard.transfer((Connection)dbmsConnection, (String)fileString, "lobs");
        } catch (SiardError e) {
            throw new SemanticError(n, "SIARD transfer failed: " + e.getReason());
        }
        MdObject md = (MdObject)connections.getValue(connectionString);
        if (md != null) {
            log.write(Log.DEBUG, "%s* SIARD metadata: %s\n", indent(level), md.toString());
            SiardMetadata.updateMetadata((String)fileString, md);
            log.write(Log.DEBUG, "%s* ROAE output to '%s'\n", indent(level), (String)roaeFileString);
            RoaeMetadata.updateMetadata(roaeFileString, md);
        } else {
            log.write(Log.WARNING, "%s* SIARD metadata not found\n", indent(level));
        }
    }

    String interpretSiardMetadataField(String fieldName, Node n, int level, Context ctx, MdObject parent) {
        Node field = n.getChildByFieldName(fieldName);
        if (field == null) {
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

    void interpretSiardMetadata(Node n, int level, Context ctx) {
        Node connection = n.getChildByFieldName("connection");
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
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_schema")) {
                interpretSiardSchema(c, level + 1, ctx, md);
            } else if (c.getType().equals("command_declaration")) {
                interpretCommandDeclaration(c, level + 1, ctx, md);
            }
        }
    }

    void interpretSiardSchema(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.SCHEMA, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD schema: %s [%s]\n", indent(level), nameString, descriptionString);
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_type")) {
                interpretSiardType(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_table")) {
                interpretSiardTable(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_view")) {
                interpretSiardView(c, level + 1, ctx, md);
            }
        }
    }

    void interpretSiardType(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.TYPE, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD type: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardTable(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.TABLE, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD table: %s [%s]\n", indent(level), nameString, descriptionString);
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_column")) {
                interpretSiardColumn(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_key")) {
                interpretSiardKey(c, level + 1, ctx, md);
            } else if (c.getType().equals("siard_check")) {
                interpretSiardCheck(c, level + 1, ctx, md);
            }
        }
    }

    void interpretSiardColumn(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.COLUMN, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD column: %s [%s]\n", indent(level), nameString, descriptionString);
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, md);
            }
        }
    }

    void interpretSiardField(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.FIELD, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD field: %s [%s]\n", indent(level), nameString, descriptionString);
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_field")) {
                interpretSiardField(c, level + 1, ctx, parent);
            }
        }
    }

    void interpretSiardKey(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.KEY, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD key: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardCheck(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.CHECK, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD check: %s [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretSiardView(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        String descriptionString = interpretShortDescr(n, level, ctx);
        if (descriptionString == null) {
            descriptionString = interpretSiardMetadataField("description", n, level, ctx, null);
        }
        MdObject md = new MdObject(MdType.VIEW, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* SIARD view: %s [%s]\n", indent(level), nameString, descriptionString);
        for (Node c : n.getChildren()) {
            if (c.getType().equals("siard_column")) {
                interpretSiardColumn(c, level + 1, ctx, md);
            }
        }
    }

    void interpretCommandDeclaration(Node n, int level, Context ctx, MdObject parent) {
        Node title = n.getChildByFieldName("title");
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
        Node parameters = n.getChildByFieldName("parameters");
        interpretCommandParameters(parameters, level + 1, ctx, md);
        Node body = n.getChildByFieldName("body");
        String bodyString = interpretRaw(body, level + 1, new Context(ctx, true));
        MdObject mdSql= new MdObject(MdType.SQL, "", bodyString);
        md.add(mdSql);
        log.write(Log.DEBUG, "%s'%s'\n", indent(level), bodyString);
    }

    void interpretCommandParameters(Node n, int level, Context ctx, MdObject parent) {
        log.write(Log.DEBUG, "%s* parameters\n", indent(level));
        for (Node c : n.getChildren()) {
            if (c.getType().equals("parameter")) {
                interpretCommandParameter(c, level + 1, ctx, parent);
            } else {
                throw new AstError(n);
            }
        }
    }

    void interpretCommandParameter(Node n, int level, Context ctx, MdObject parent) {
        Node name = n.getChildByFieldName("name");
        String nameString = interpretIdentifier(name, level + 1);
        ctx.setValue(nameString, config.getProperty(nameString));
        Node description = n.getChildByFieldName("description");
        String descriptionString = "";
        if (description != null && description.getType().equals("short_description")) {
            descriptionString = interpretShortDescription(description, level + 1);
        }
        MdObject md = new MdObject(MdType.PARAMETER, nameString, descriptionString);
        parent.add(md);
        log.write(Log.DEBUG, "%s* parameter: %s  [%s]\n", indent(level), nameString, descriptionString);
    }

    void interpretForLoop(Node n, int level, Context ctx) {
        Node variables = n.getChildByFieldName("variables");
        List<String> variablesStrings = interpretForVariables(variables, level + 1);
        Node resultSet = n.getChildByFieldName("result_set");
        String resultSetString = interpretIdentifier(resultSet, level + 1);
        Node body = n.getChildByFieldName("body");
        log.write(Log.DEBUG, "%s* for_loop: %s in %s\n", indent(level), variablesStrings.stream().collect(Collectors.joining(", ")), resultSetString);
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

    List<String> interpretForVariables(Node n, int level) {
        List<String> variables = new ArrayList<String>();
        for (Node c : n.getChildren()) {
            if (c.getType().equals("identifier")) {
                variables.add(interpretIdentifier(c, level + 1));
            } else {
                throw new AstError(n);
            }
        }
        return variables;
    }

    void interpretConditional(Node n, int level, Context ctx) {
        Node condition = n.getChildByFieldName("condition");
        Boolean comparisonValue = interpretComparison(condition, level + 1, ctx);
        log.write(Log.DEBUG, "* Conditional: value = '%s'\n", comparisonValue.booleanValue());
        if (comparisonValue.booleanValue()) {
            Node thenBlock = n.getChildByFieldName("then");
            interpretStatementBlock(thenBlock, level + 1, ctx);
        } else {
            Node elseBlock = n.getChildByFieldName("else");
            if (elseBlock != null) {
                interpretStatementBlock(elseBlock, level + 1, ctx);
            }
        }
    }

    void interpretStatementBlock(Node n, int level, Context ctx) {
        log.write(Log.DEBUG, "%s* statement_block\n", indent(level));
        for (Node c : n.getChildren()) {
            interpretStatement(c, level, ctx);
        }
    }

    Boolean interpretComparison(Node n, int level, Context ctx) {
        Node left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        Node operator = n.getChildByFieldName("operator");
        String operatorString = interpretComparisonOperator(operator, level + 1);
        Node right = n.getChildByFieldName("right");
        Object rightValue = interpretBasicExpression(right, level + 1, ctx);
        if (!(leftValue instanceof BigInteger)) {
            throw new SemanticError(n, "Left side of comparison is not integer");
        }
        if (!(rightValue instanceof BigInteger)) {
            throw new SemanticError(n, "Right side of comparison is not integer");
        }
        int comparisonValue = ((BigInteger)leftValue).compareTo((BigInteger)rightValue);
        if (operatorString.equals("==")) {
            return comparisonValue == 0;
        } else if (operatorString.equals("!=")) {
            return comparisonValue != 0;
        } else if (operatorString.equals("<")) {
            return comparisonValue == -1;
        } else if (operatorString.equals(">")) {
            return comparisonValue == 1;
        } else if (operatorString.equals("<=")) {
            return comparisonValue < 1;
        } else if (operatorString.equals(">=")) {
            return comparisonValue > -1;
        } else {
            throw new SemanticError(n, "Unknown comparison operator: " + operatorString);
        }
    }

    Object interpretBasicExpression(Node n, int level, Context ctx) {
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

    Object interpretVariableInstance(Node n, int level, Context ctx) {
        Node identifier = n.getChild(0);
        String identifierName = interpretIdentifier(identifier, level + 1);
        Object result = ctx.getValue(identifierName);
        if (result == null) {
            throw new SemanticError(n, "The variable '" + identifierName + "' has not been set.");
        }
        return result;
    }

    Object interpretDotExpression(Node n, int level, Context ctx) {
        Node left = n.getChildByFieldName("left");
        Object leftValue = interpretBasicExpression(left, level + 1, ctx);
        Node right = n.getChildByFieldName("right");
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

    String interpretString(Node n, int level, Context ctx) {
        String result = "";
        for (Node c : n.getChildren()) {
            if (c.getType().equals("interpolation")) {
                result += interpretInterpolation(c, level + 1, ctx);
            } else if (c.getType().equals("escape_sequence")) {
                result += interpretEscapeSequence(c, level + 1);
            } else if (c.getType().equals("string_content")) {
                result += interpretStringContent(c, level + 1);
            } else {
                throw new AstError(n);
            }
        }
        return result;
    }

    String interpretShortDescr(Node n, int level, Context ctx) {
        Node description = n.getChildByFieldName("description");
        return description != null ? interpretShortDescription(description, level + 1) : null;
    }

    String interpretInterpolation(Node n, int level, Context ctx) {
        return interpretInterpolationCommon(n, level, ctx);
    }

    String interpretInterpolation2(Node n, int level, Context ctx) {
        return interpretInterpolationCommon(n, level, ctx);
    }

    String interpretInterpolationCommon(Node n, int level, Context ctx) {
        Object interpolatedObject = interpretBasicExpression(n.getChild(0), level, ctx);
        if (interpolatedObject instanceof String) {
            return (String)interpolatedObject;
        } else if (interpolatedObject instanceof BigInteger) {
            return String.valueOf((BigInteger)interpolatedObject);
        } else {
            throw new SemanticError(n, "Interpolation must be String or BigInteger");
        }
    }

    Context interpretKeyValuePairs(Node n, int level, Context ctx) {
        Context keyValuePairs = new Context(); // NB: does not inherit from ctx
        if (n != null) {
            for (Node c : n.getChildren()) {
                if (c.getType().equals("key_value_pair")) {
                    interpretKeyValuePair(c, level + 1, keyValuePairs, ctx);
                } else {
                    throw new AstError(n);
                }
            }
        }
        return keyValuePairs;
    }

    void interpretKeyValuePair(Node n, int level, Context keyValuePairs, Context ctx) {
        Node key = n.getChildByFieldName("key");
        String keyString = interpretIdentifier(key, level + 1);
        Node value = n.getChildByFieldName("value");
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

    String interpretRaw(Node n, int level, Context ctx) {
        String result = "";
        int trailingNewlines = 0;
        for (Node c : n.getChildren()) {
            if (c.getType().equals("raw_content")) {
                String rc = interpretRawContent(c, level + 1);
                result += rc;
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
                result += interpretInterpolation(c, level + 1, ctx);
            } else if (c.getType().equals("interpolation2")) {
                result += interpretInterpolation2(c, level + 1, ctx);
            } else {
                throw new AstError(n);
            }
        }
        // Trim trailing newlines stemming from newlines after raw.
        // This is an issue since we allow raw sections to continue after
        // non-indented empty lines.
        return result.substring(0, result.length() - trailingNewlines);
    }

    // Methods corresponding to terminal AST nodes

    String interpretIdentifier(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretShortDescription(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretEscapeSequence(Node n, int level) {
        return StringEscapeUtils.unescapeJava(source.substring(n.getStartByte(), n.getEndByte()));
    }

    String interpretStringContent(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretRawContent(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretComparisonOperator(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    String interpretDotOperator(Node n, int level) {
        return source.substring(n.getStartByte(), n.getEndByte());
    }

    BigInteger interpretInteger(Node n, int level) {
        String content = source.substring(n.getStartByte(), n.getEndByte());
        return new BigInteger(content);
    }
}
