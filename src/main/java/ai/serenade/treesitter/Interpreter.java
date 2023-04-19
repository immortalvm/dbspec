package ai.serenade.treesitter;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

import ai.serenade.treesitter.MdObject.MdType;


public class Interpreter {
	String source;
	Tree tree;
	Context context;
	Context connections;
	Dbms dbms;
	Siard siard;
	Properties config = new Properties();
	final static String CONFIG_FILENAME = "dbspec.conf";
	final static String TREE_SITTER_LIBRARY = "../iDA-DbSpec-interpreter/libjava-tree-sitter.so";
	
	static {
		System.load(FileSystems.getDefault().getPath(TREE_SITTER_LIBRARY).normalize().toAbsolutePath().toString());
	}

	Interpreter(String source) {
		try {
			this.source = source;
			this.context = new Context();
			this.connections = new Context();
			this.dbms = new Dbms();
			this.siard = new Siard(this.dbms);
			loadConfigFile();
			Parser parser = new Parser();
			parser.setLanguage(Languages.dbspec());
			tree = parser.parseString(source);
			System.out.format("AST: %s\n\n", tree.getRootNode().getNodeString());
			parser.close();
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported encoding");
		}
	}
	
	void loadConfigFile() {
		try {
			config.load(new FileInputStream(CONFIG_FILENAME));
		} catch (Exception e) {
			System.out.format("Unable to read configuration file '%s'\n", CONFIG_FILENAME);
		}
	}
	
	void interpret() {
		Node n = tree.getRootNode();
		System.out.format("Interpretation:\n\n");
		interpretSourceFile(n, 0, context);
	}

	void indent(int level) {
		for (int i = 0; i < level; i++) {
			System.out.print("  ");
		}
	}

	// Methods corresponding to non-terminal AST nodes
	
	void interpretSourceFile(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("* source file\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("parameters")) {
				interpretParameters(c, level + 1, ctx);
			} else {
				interpretStatement(c, level, ctx);
			}
		}
	}
	
	void interpretParameters(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("* parameters\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("parameter")) {
				interpretParameter(c, level + 1, ctx);
			} else {
				throw new AstError();
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
		indent(level);
		System.out.format("* parameter: %s  [%s]\n", nameString, descriptionString);
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
			throw new AstError();
		}
	}

	void interpretLog(Node n, int level, Context ctx) {
		Object logValue = interpretBasicExpression(n.getChild(0), level + 1, ctx);
		indent(level);
		System.out.format("* Log message: '%s'\n", logValue);
	}

	void interpretAssert(Node n, int level, Context ctx) {
		boolean comparisonValue = interpretComparison(n.getChild(0), level, ctx);
		indent(level);
		System.out.format("* Assertion: '%s'\n", comparisonValue);
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
			throw new AstError();
		}
		indent(level);
		System.out.format("* Set %s = '%s'\n", variableName, variableValue);
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
			throw new SemanticError("Interpreter is not a string");
		}
		Node script = n.getChildByFieldName("script");
		String scriptString = interpretRaw(script, level + 1, ctx);
		indent(level);
		System.out.format("* Executing using interpreter '%s': '%s'\n", (String)interpreterString, scriptString);
	}
	
	String interpretScriptResult(Node n, int level, Context ctx) {
		Node interpreter = n.getChildByFieldName("interpreter");
		Object interpreterString = interpretBasicExpression(interpreter, level, ctx);
		if (!(interpreterString instanceof String)) {
			throw new SemanticError("Interpreter is not a string");
		}
		Node script = n.getChildByFieldName("script");
		String scriptString = interpretRaw(script, level + 1, ctx);
		indent(level);
		String scriptResult = Script.execute((String)interpreterString, scriptString); 
		System.out.format("* Executing using interpreter %s: '%s'\n", (String)interpreterString, scriptString);
		return scriptResult;
	}

	Connection interpretConnection(Node n, int level, Context ctx) {
		Node url = n.getChildByFieldName("url");
		Object urlString = interpretBasicExpression(url, level, ctx);
		if (!(urlString instanceof String)) {
			throw new SemanticError("URL is not a string");
		}
		Node properties = n.getChildByFieldName("properties");
		indent(level);
		System.out.format("* connection: %s\n", (String)urlString);
		Context connectionContext = interpretKeyValuePairs(properties, level + 1, ctx);
		return dbms.connect((String)urlString, connectionContext);
	}
	
	void interpretExecuteSql(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		String connectionString = interpretIdentifier(connection, level + 1);
		Object dbmsConnection = ctx.getValue(connectionString);
		if (!(dbmsConnection instanceof Connection)) {
			throw new SemanticError("Connection variable does not refer to an SQL connection");
		}
		Node sql = n.getChildByFieldName("sql");
		String sqlString = interpretRaw(sql, level + 1, ctx);
		indent(level);
		System.out.format("* Executing SQL on connection %s: '%s'\n", connectionString, sqlString);
		dbms.executeSqlUpdate((Connection)dbmsConnection, sqlString);
	}
	
	ResultSet interpretQuery(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		String connectionString = interpretIdentifier(connection, level + 1);
		Object dbmsConnection = ctx.getValue(connectionString);
		if (!(dbmsConnection instanceof Connection)) {
			throw new SemanticError("Connection variable does not refer to an SQL connection");
		}
		Node sql = n.getChildByFieldName("sql");
		String sqlString = interpretRaw(sql, level + 1, ctx);
		indent(level);
		System.out.format("* Executing SQL query on connection %s: '%s'\n", connectionString, sqlString);
		return dbms.executeSqlQuery((Connection)dbmsConnection, sqlString);
	}
	
	void interpretSiardOutput(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		String connectionString = interpretIdentifier(connection, level + 1);
		Object dbmsConnection = ctx.getValue(connectionString);
		if (!(dbmsConnection instanceof Connection)) {
			throw new SemanticError("Connection variable does not refer to an SQL connection");
		}
		Node file = n.getChildByFieldName("file");
		Object fileString = interpretBasicExpression(file, level, ctx);
		if (!(fileString instanceof String)) {
			throw new SemanticError("Filename is not a string");
		}
		indent(level);
		System.out.format("* SIARD output %s to '%s'\n", connectionString, (String)fileString);
		try {
			siard.transfer((Connection)dbmsConnection, (String)fileString, "lobs");
		} catch (SiardError e) {
			throw new SemanticError("SIARD transfer failed: " + e.getReason());
		}
		MdObject md = (MdObject)connections.getValue(connectionString);
		System.out.format("* SIARD metadata: %s\n", md.toString());
		SiardMetadata.updateMetadata((String)fileString, md);
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
			throw new SemanticError("Siard field " + fieldName + " is not a string");
		}
		indent(level);
		System.out.format("* SIARD metadata %s: %s\n", fieldName, (String)fieldString);
		MdObject md = new MdObject(MdType.INFO, fieldName, (String)fieldString);
		parent.add(md);
		return (String)fieldString;
	}
	
	void interpretSiardMetadata(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		String connectionString = interpretIdentifier(connection, level + 1);
		MdObject md = new MdObject(MdType.METADATA, "", "");
		connections.setValue(connectionString, md);
		indent(level);
		System.out.format("* SIARD metadata for %s\n", connectionString);
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
				interpretCommandDeclaration(c, level + 1, ctx);
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
		indent(level);
		System.out.format("* SIARD schema: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD type: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD table: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD column: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD field: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD key: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD check: %s [%s]\n", nameString, descriptionString);
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
		indent(level);
		System.out.format("* SIARD view: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_column")) {
				interpretSiardColumn(c, level + 1, ctx, md);
			}
		}
	}
	
	void interpretCommandDeclaration(Node n, int level, Context ctx) {
		Node title = n.getChildByFieldName("title");
		Object titleString = null;
		if (title.getType().equals("raw")) {
			titleString = interpretRaw(title, level, ctx);
		} else {
			titleString = interpretBasicExpression(title, level, ctx);
		}
		if (!(titleString instanceof String)) {
			throw new SemanticError("Title is not a string");
		}
		indent(level);
		System.out.format("* Command declaration: %s\n", (String)titleString);
		Node parameters = n.getChildByFieldName("parameters");
		interpretParameters(parameters, level + 1, ctx);
		Node body = n.getChildByFieldName("body");
		String bodyString = interpretRaw(body, level + 1, new Context(ctx, true));
		indent(level);
		System.out.format("'%s'\n", bodyString);
	}
	
	void interpretForLoop(Node n, int level, Context ctx) {
		Node variables = n.getChildByFieldName("variables");
		List<String> variablesStrings = interpretForVariables(variables, level + 1);
		Node resultSet = n.getChildByFieldName("result_set");
		String resultSetString = interpretIdentifier(resultSet, level + 1);
		Node body = n.getChildByFieldName("body");
		indent(level);
		System.out.format("* for_loop: %s in %s\n", variablesStrings.stream().collect(Collectors.joining(", ")), resultSetString);
		ResultSet rs = (ResultSet)(ctx.getValue(resultSetString));
		try {
			ResultSetMetaData rsmeta = rs.getMetaData();
			int ncols = Math.min(variablesStrings.size(), rsmeta.getColumnCount());
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
			throw new SemanticError("Error in ResultSet.");
		}
	}

	List<String> interpretForVariables(Node n, int level) {
		List<String> variables = new ArrayList<String>();
		for (Node c : n.getChildren()) {
			if (c.getType().equals("identifier")) {
				variables.add(interpretIdentifier(c, level + 1));
			} else {
				throw new AstError();
			}
		}
		return variables;
	}
	
	void interpretConditional(Node n, int level, Context ctx) {
		Node condition = n.getChildByFieldName("condition");
		Boolean comparisonValue = interpretComparison(condition, level + 1, ctx);
		System.out.format("* Conditional: value = '%s'\n", comparisonValue.booleanValue());
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
		indent(level);
		System.out.format("* statement_block\n");
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
			throw new SemanticError("Left side of comparison is not integer");
		}
		if (!(rightValue instanceof BigInteger)) {
			throw new SemanticError("Left side of comparison is not integer");
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
			throw new SemanticError("Unknown comparison operator: " + operatorString);
		}
	}
	
	Object interpretBasicExpression(Node n, int level, Context ctx) {
		if (n.getType().equals("string")) {
			return interpretString(n, level + 1, ctx);
		} else if (n.getType().equals("variable_instance")) {
			Object result = interpretVariableInstance(n, level + 1, ctx); 
			return result != null ? result : "<undefined>";
		} else if (n.getType().equals("integer")) {
			return interpretInteger(n, level + 1);
		} else if (n.getType().equals("dot_expression")) {
			return interpretDotExpression(n, level + 1, ctx);
		} else {
			throw new AstError();
		}
	}
	
	Object interpretVariableInstance(Node n, int level, Context ctx) {
		Node identifier = n.getChild(0);
		String identifierName = interpretIdentifier(identifier, level + 1);
		return ctx.getValue(identifierName);
	}
	
	Object interpretDotExpression(Node n, int level, Context ctx) {
		Node left = n.getChildByFieldName("left");
		Object leftValue = interpretBasicExpression(left, level + 1, ctx);
		Node right = n.getChildByFieldName("right");
		String rightOperator = interpretDotOperator(right, level + 1);
		System.out.format("* Dot expression: value = '%s'.'%s'\n", leftValue, rightOperator);
		if (leftValue instanceof String) {
			if (rightOperator.equals("stripped")) {
				return ((String)leftValue).trim();
			} else if (rightOperator.equals("as_integer")) {
				return new BigInteger((String)leftValue);
			} else {
				throw new SemanticError("Unsupported dot expression");
			}
		} else if (leftValue instanceof ResultSet) {
			if (rightOperator.equals("size")) {
				ResultSet rs = (ResultSet)leftValue;
				try {
					rs.last();
					return rs.getRow();					
				} catch (SQLException e) {
					throw new SemanticError("Error in ResultSet.");
				}
			} else {
				throw new SemanticError("Unsupported dot expression");
			}
		} else {
			throw new SemanticError("Illegal type in dot expression");
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
				throw new AstError();
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
			throw new SemanticError("Interpolation must be String or BigInteger");
		}
	}
	
	Context interpretKeyValuePairs(Node n, int level, Context ctx) {
		Context keyValuePairs = new Context();
		for (Node c : n.getChildren()) {
			if (c.getType().equals("key_value_pair")) {
				interpretKeyValuePair(c, level + 1, keyValuePairs, ctx);
			} else {
				throw new AstError();
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
			throw new SemanticError("Value is not a string");
		}
		keyValuePairs.setValue(keyString, (String)valueString);
	}

	String interpretRaw(Node n, int level, Context ctx) {
		String result = ""; 
		for (Node c : n.getChildren()) {
			if (c.getType().equals("raw_content")) {
				result += interpretRawContent(c, level + 1);
			} else if (c.getType().equals("interpolation")) {
				result += interpretInterpolation(c, level + 1, ctx);
			} else if (c.getType().equals("interpolation2")) {
				result += interpretInterpolation2(c, level + 1, ctx);
			} else {
				throw new AstError();
			}
		}
		return result;
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
