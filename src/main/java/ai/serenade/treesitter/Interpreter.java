package ai.serenade.treesitter;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

public class Interpreter {
	String source;
	Tree tree;
	Context context;
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
			loadConfigFile();
			Parser parser = new Parser();
			parser.setLanguage(Languages.dbspec());
			tree = parser.parseString(source);
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
			if (c.getType().equals("nop")) {
				// NOP
			} else if (c.getType().equals("parameters")) {
				interpretParameters(c, level + 1, ctx);
			} else if (c.getType().equals("set")) {
				interpretSet(c, level + 1, ctx);
			} else if (c.getType().equals("execute_using")) {
				interpretExecuteUsing(c, level + 1, ctx);
			} else if (c.getType().equals("execute_sql")) {
				interpretExecuteSql(c, level + 1, ctx);
			} else if (c.getType().equals("siard_metadata")) {
				interpretSiardMetadata(c, level + 1, ctx);
			} else if (c.getType().equals("siard_output")) {
				interpretSiardOutput(c, level + 1, ctx);
			} else if (c.getType().equals("for_loop")) {
				interpretForLoop(c, level + 1, ctx);
			} else {
				throw new AstError();
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
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		ctx.setValue(nameString, config.getProperty(nameString));
		String descriptionString = "";
		if (description.getType().equals("short_description")) {
			descriptionString = interpretShortDescription(description, level + 1);
		}
		indent(level);
		System.out.format("* parameter: %s  [%s]\n", nameString, descriptionString);
	}
	
	void interpretSet(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node value = n.getChildByFieldName("value");
		String variableName = interpretIdentifier(name, level + 1);
		Object variableValue = null;
		if (value.getType().equals("string")) {
			variableValue = interpretString(value, level + 1, ctx);
		} else if (value.getType().equals("variable_instance")) {
			variableValue = interpretVariableInstance(value, level + 1, ctx);
		} else if (value.getType().equals("connection")) {
			variableValue = interpretConnection(value, level + 1, ctx);
		} else if (value.getType().equals("query")) {
			variableValue = interpretQuery(value, level + 1, ctx);
		} else {
			throw new AstError();
		}
		indent(level);
		System.out.format("* Set %s = '%s'\n", variableName, variableValue);
		ctx.setValue(variableName, variableValue);
	}

	void interpretExecuteUsing(Node n, int level, Context ctx) {
		Node interpreter = n.getChildByFieldName("interpreter");
		Node script = n.getChildByFieldName("script");
		String interpreterString;
		if (interpreter.getType().equals("string")) {
			interpreterString = interpretString(interpreter, level + 1, ctx);
		} else if (interpreter.getType().equals("variable_instance")) {
			interpreterString = interpretVariableInstance(interpreter, level + 1, ctx);
		} else {
			throw new AstError();
		}
		String scriptString = interpretRaw(script, level + 1, ctx);
		indent(level);
		System.out.format("* Executing using interpreter %s: '%s'\n", interpreterString, scriptString);
	}

	Connection interpretConnection(Node n, int level, Context ctx) {
		String urlString;
		Node url = n.getChildByFieldName("url");
		Node properties = n.getChildByFieldName("properties");
		if (url.getType().equals("string")) {
			urlString = interpretString(url, level + 1, ctx);
		} else if (url.getType().equals("variable_instance")) {
			urlString = interpretVariableInstance(url, level + 1, ctx);
		} else {
			throw new AstError();
		}
		indent(level);
		System.out.format("* connection: %s\n", urlString);
		return new Connection(urlString, interpretKeyValuePairs(properties, level + 1, ctx));
	}
	
	void interpretExecuteSql(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		Node sql = n.getChildByFieldName("sql");
		String connectionString = interpretIdentifier(connection, level + 1);
		String sqlString = interpretRaw(sql, level + 1, ctx);
		indent(level);
		System.out.format("* Executing sql on connection %s: '%s'\n", connectionString, sqlString);
	}
	
	ResultSet interpretQuery(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		Node sql = n.getChildByFieldName("sql");
		String connectionString = interpretIdentifier(connection, level + 1);
		String sqlString = interpretRaw(sql, level + 1, ctx);
		indent(level);
		System.out.format("* Executing query on connection %s: '%s'\n", connectionString, sqlString);
		return Query.getResultSet(sqlString);
	}

	void interpretSiardOutput(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		Node name = n.getChildByFieldName("name");
		Node file = n.getChildByFieldName("file");
		String connectionString = interpretIdentifier(connection, level + 1);
		String nameString = interpretIdentifier(name, level + 1);
		String fileString;
		if (file.getType().equals("string")) {
			fileString = interpretString(file, level + 1, ctx);
		} else if (file.getType().equals("variable_instance")) {
			fileString = interpretVariableInstance(file, level + 1, ctx);
		} else {
			throw new AstError();
		}
		indent(level);
		System.out.format("* SIARD output %s.%s to '%s'\n", connectionString, nameString, fileString);
		
	}

	// Helper method
	void interpretSiardMetadataField(String fieldName, Node n, int level, Context ctx) {
		Node field = n.getChildByFieldName(fieldName);
		if (field.getType().equals("")) {
			// Skip non-existent SIARD field
			return;
		}
		String fieldString;
		if (field.getType().equals("raw")) {
			fieldString = interpretRaw(field, level + 1, ctx);
		} else if (field.getType().equals("string")) {
			fieldString = interpretString(field, level + 1, ctx);
		} else if (field.getType().equals("variable_instance")) {
			fieldString = interpretVariableInstance(field, level + 1, ctx);
		} else {
			throw new AstError();
		}
		indent(level);
		System.out.format("* SIARD metadata %s: %s\n", fieldName, fieldString);
	}
	
	void interpretSiardMetadata(Node n, int level, Context ctx) {
		Node connection = n.getChildByFieldName("connection");
		Node name = n.getChildByFieldName("name");
		String connectionString = interpretIdentifier(connection, level + 1);
		String nameString = interpretIdentifier(name, level + 1);
		indent(level);
		System.out.format("* SIARD metadata for %s.%s\n", connectionString, nameString);
		interpretSiardMetadataField("dbname", n, level + 1, ctx);
		interpretSiardMetadataField("description", n, level + 1, ctx);
		interpretSiardMetadataField("archiver", n, level + 1, ctx);
		interpretSiardMetadataField("archiverContact", n, level + 1, ctx);
		interpretSiardMetadataField("dataOwner", n, level + 1, ctx);
		interpretSiardMetadataField("dataOriginTimespan", n, level + 1, ctx);
		interpretSiardMetadataField("lobFolder", n, level + 1, ctx);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_schema")) {
				interpretSiardSchema(c, level + 1, ctx);
			}
		}
	}
	
	// Helper method
	String interpretDescriptionString(Node description, int level, boolean required, Context ctx) {
		String descriptionString = "";
		if (description.getType().equals("short_description")) {
			descriptionString = interpretShortDescription(description, level);
		} else if (description.getType().equals("raw")) {
			descriptionString = interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			descriptionString = interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			descriptionString = interpretVariableInstance(description, level, ctx);
		} else if (required) {
			throw new AstError();
		}
		return descriptionString;
	}
	
	void interpretSiardSchema(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD schema: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_type")) {
				interpretSiardType(c, level + 1, ctx);
			} else if (c.getType().equals("siard_table")) {
				interpretSiardTable(c, level + 1, ctx);
			} else if (c.getType().equals("siard_view")) {
				interpretSiardView(c, level + 1, ctx);
			}
		}
	}

	void interpretSiardType(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD type: %s [%s]\n", nameString, descriptionString);
	}
	
	void interpretSiardTable(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, false, ctx);
		indent(level);
		System.out.format("* SIARD table: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_column")) {
				interpretSiardColumn(c, level + 1, ctx);
			} else if (c.getType().equals("siard_key")) {
				interpretSiardKey(c, level + 1, ctx);
			} else if (c.getType().equals("siard_check")) {
				interpretSiardCheck(c, level + 1, ctx);
			}
		}
	}

	void interpretSiardColumn(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, false, ctx);
		indent(level);
		System.out.format("* SIARD column: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_field")) {
				interpretSiardField(c, level + 1, ctx);
			}
		}
	}
	
	void interpretSiardField(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD field: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_field")) {
				interpretSiardField(c, level + 1, ctx);
			}
		}
	}
	
	void interpretSiardKey(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD key: %s [%s]\n", nameString, descriptionString);
	}

	void interpretSiardCheck(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD check: %s [%s]\n", nameString, descriptionString);
	}
	
	void interpretSiardView(Node n, int level, Context ctx) {
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		String nameString = interpretIdentifier(name, level + 1);
		String descriptionString = interpretDescriptionString(description, level, true, ctx);
		indent(level);
		System.out.format("* SIARD view: %s [%s]\n", nameString, descriptionString);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_column")) {
				interpretSiardColumn(c, level + 1, ctx);
			}
		}
	}
	
	void interpretForLoop(Node n, int level, Context ctx) {
		Node variables = n.getChildByFieldName("variables");
		Node resultSet = n.getChildByFieldName("result_set");
		Node body = n.getChildByFieldName("body");
		List<String> variablesStrings = interpretForVariables(variables, level + 1);
		String resultSetString = interpretIdentifier(resultSet, level + 1);
		indent(level);
		System.out.format("* for_loop: %s in %s\n", variablesStrings.stream().collect(Collectors.joining(", ")), resultSetString);
		for (ResultRow row : ((ResultSet)ctx.getValue(resultSetString)).getRows()) {
			Context newCtx = new Context(ctx);
			int i = 0;
			for (String variableString : variablesStrings) {
				newCtx.setValue(variableString, row.getString(i));
				i++;
			}
			interpretForBody(body, level + 1, newCtx);
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
	
	void interpretForBody(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("* for_body\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("nop")) {
				// NOP
			} else if (c.getType().equals("set")) {
				interpretSet(c, level + 1, ctx);
			} else if (c.getType().equals("execute_using")) {
				interpretExecuteUsing(c, level + 1, ctx);
			} else if (c.getType().equals("execute_sql")) {
				interpretExecuteSql(c, level + 1, ctx);
			} else if (c.getType().equals("siard_metadata")) {
				interpretSiardMetadata(c, level + 1, ctx);
			} else if (c.getType().equals("siard_output")) {
				interpretSiardOutput(c, level + 1, ctx);
			} else if (c.getType().equals("for_loop")) {
				interpretForLoop(c, level + 1, ctx);
			} else {
				throw new AstError();
			}
		}
	}
	
	String interpretVariableInstance(Node n, int level, Context ctx) {
		String result = "";
		for (Node c : n.getChildren()) {
			String variableName = interpretIdentifier(c, level + 1);
			result = (String)ctx.getValue(variableName);
			break;
		}
		return result;
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
	
	String interpretInterpolation(Node n, int level, Context ctx) {
		String result = "";
		for (Node c : n.getChildren()) {
			if (c.getType().equals("string")) {
				result = interpretString(c, level + 1, ctx);
				break;
			} else if (c.getType().equals("variable_instance")) {
				result = interpretVariableInstance(c, level + 1, ctx);
				break;
			} else {
				throw new AstError();
			}
		}
		return result;
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
		Node value = n.getChildByFieldName("value");
		String keyString = interpretIdentifier(key, level + 1);
		String valueString;
		if (value.getType().equals("raw")) {
			valueString = interpretRaw(value, level + 1, ctx);
		} else if (value.getType().equals("string")) {
			valueString = interpretString(value, level + 1, ctx);
		} else if (value.getType().equals("variable_instance")) {
			valueString = interpretVariableInstance(value, level + 1, ctx);
		} else {
			throw new AstError();
		}
		keyValuePairs.setValue(keyString, valueString);
	}

	String interpretRaw(Node n, int level, Context ctx) {
		String result = ""; 
		for (Node c : n.getChildren()) {
			if (c.getType().equals("raw_content")) {
				result += interpretRawContent(c, level + 1);
			} else if (c.getType().equals("interpolation")) {
				result += interpretInterpolation(c, level + 1, ctx);
			} else {
				throw new AstError();
			}
		}
		return result;
	}
	
	// Methods corresponding to terminal AST nodes
	
	String interpretIdentifier(Node n, int level) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		return contents;
	}
	
	String interpretShortDescription(Node n, int level) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		return contents;
	}
	
	String interpretEscapeSequence(Node n, int level) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		return StringEscapeUtils.unescapeJava(contents);
	}
	
	String interpretStringContent(Node n, int level) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		return contents;
	}
	
	String interpretRawContent(Node n, int level) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		return contents;
	}
	
}
