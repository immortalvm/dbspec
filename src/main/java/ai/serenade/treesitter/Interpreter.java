package ai.serenade.treesitter;

import java.io.UnsupportedEncodingException;

public class Interpreter {
	String source;
	Tree tree;
	Context variables;
	
	static {
		System.load("/home/thor/proj/idapiql/src/iDA-DbSpec-interpreter/libjava-tree-sitter.so");
	}

	Interpreter(String source) {
		try {
			this.source = source;
			this.variables = null;
			Parser parser = new Parser();
			parser.setLanguage(Languages.dbspec());
			tree = parser.parseString(source);
			System.out.format("AST: %s\n\n", tree.getRootNode().getNodeString());
			parser.close();
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported encoding");
		}
	}
	
	void interpret() {
		Node n = tree.getRootNode();
		System.out.format("Interpretation:\n\n");
		interpretSourceFile(n, 0, null);
	}

	void indent(int level) {
		for (int i = 0; i < level; i++) {
			System.out.print("  ");
		}
	}

	// Methods corresponding to productions in grammar
	
	void interpretSourceFile(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("source file\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("parameters")) {
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
			}
		}
	}
	
	void interpretParameters(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("parameters\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("parameter")) {
				interpretParameter(c, level + 1, ctx);
			}
		}
	}
	
	void interpretParameter(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("parameter\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level + 1, ctx);
		}
	}
	
	void interpretSet(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("set\n");
		Node name = n.getChildByFieldName("name");
		Node value = n.getChildByFieldName("value");
		interpretIdentifier(name, level + 1, ctx);
		if (value.getType().equals("string")) {
			interpretString(value, level + 1, ctx);
		} else if (value.getType().equals("variable_instance")) {
			interpretVariableInstance(value, level + 1, ctx);
		} else if (value.getType().equals("connection")) {
			interpretConnection(value, level + 1, ctx);
		} else if (value.getType().equals("query")) {
			interpretQuery(value, level + 1, ctx);
		} 
	}

	void interpretExecuteUsing(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("execute_using\n");
		Node interpreter = n.getChildByFieldName("interpreter");
		Node script = n.getChildByFieldName("script");
		if (interpreter.getType().equals("string")) {
			interpretString(interpreter, level + 1, ctx);
		} else if (interpreter.getType().equals("variable_instance")) {
			interpretVariableInstance(interpreter, level + 1, ctx);
		}
		interpretRaw(script, level + 1, ctx);
	}

	void interpretConnection(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("connection\n");
		Node url = n.getChildByFieldName("url");
		Node properties = n.getChildByFieldName("properties");
		if (url.getType().equals("string")) {
			interpretString(url, level + 1, ctx);
		} else if (url.getType().equals("variable_instance")) {
			interpretVariableInstance(url, level + 1, ctx);
		}
		interpretKeyValuePairs(properties, level + 1, ctx);
	}
	
	void interpretExecuteSql(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("execute_sql\n");
		Node connection = n.getChildByFieldName("connection");
		Node sql = n.getChildByFieldName("sql");
		interpretIdentifier(connection, level + 1, ctx);
		interpretRaw(sql, level + 1, ctx);
	}
	
	void interpretQuery(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("query\n");
		Node connection = n.getChildByFieldName("connection");
		Node sql = n.getChildByFieldName("sql");
		interpretIdentifier(connection, level + 1, ctx);
		interpretRaw(sql, level + 1, ctx);
	}

	void interpretSiardOutput(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD output\n");
		Node connection = n.getChildByFieldName("connection");
		Node name = n.getChildByFieldName("name");
		Node file = n.getChildByFieldName("file");
		interpretIdentifier(connection, level + 1, ctx);
		interpretIdentifier(name, level + 1, ctx);
		
		if (file.getType().equals("string")) {
			interpretString(file, level + 1, ctx);
		} else if (file.getType().equals("variable_instance")) {
			interpretVariableInstance(file, level + 1, ctx);
		}
	}

	// Helper method
	void interpretSiardMetadataField(String fieldName, Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD metadata %s\n", fieldName);
		Node field = n.getChildByFieldName(fieldName);
		if (field.getType().equals("raw")) {
			interpretRaw(field, level + 1, ctx);
		} else if (field.getType().equals("string")) {
			interpretString(field, level + 1, ctx);
		} else if (field.getType().equals("variable_instance")) {
			interpretVariableInstance(field, level + 1, ctx);
		}
	}
	
	void interpretSiardMetadata(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD metadata\n");
		Node connection = n.getChildByFieldName("connection");
		Node name = n.getChildByFieldName("name");
		interpretIdentifier(connection, level + 1, ctx);
		interpretIdentifier(name, level + 1, ctx);
		interpretSiardMetadataField("dbname", n, level, ctx);
		interpretSiardMetadataField("description", n, level, ctx);
		interpretSiardMetadataField("archiver", n, level, ctx);
		interpretSiardMetadataField("archiverContact", n, level, ctx);
		interpretSiardMetadataField("dataOwner", n, level, ctx);
		interpretSiardMetadataField("dataOriginTimespan", n, level, ctx);
		interpretSiardMetadataField("lobFolder", n, level, ctx);
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_schema")) {
				interpretSiardSchema(c, level + 1, ctx);
			}
		}
	}
	
	void interpretSiardSchema(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD schema\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
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
		indent(level);
		System.out.format("SIARD type\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
	}
	
	void interpretSiardTable(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD table\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
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
		indent(level);
		System.out.format("SIARD column\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_field")) {
				interpretSiardField(c, level + 1, ctx);
			}
		}
	}
	
	void interpretSiardField(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD field\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_field")) {
				interpretSiardField(c, level + 1, ctx);
			}
		}
	}
	
	void interpretSiardKey(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD key\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
	}

	void interpretSiardCheck(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD check\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
	}
	void interpretSiardView(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("SIARD view\n");
		Node name = n.getChildByFieldName("name");
		Node description = n.getChildByFieldName("description");
		interpretIdentifier(name, level + 1, ctx);
		if (description.getType().equals("short_description")) {
			interpretShortDescription(description, level, ctx);
		} else if (description.getType().equals("raw")) {
			interpretRaw(description, level, ctx);
		} else if (description.getType().equals("string")) {
			interpretString(description, level, ctx);
		} else if (description.getType().equals("variable_instance")) {
			interpretVariableInstance(description, level, ctx);
		}
		for (Node c : n.getChildren()) {
			if (c.getType().equals("siard_column")) {
				interpretSiardColumn(c, level + 1, ctx);
			}
		}
	}
	
	void interpretForLoop(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("for_loop\n");
		Node variables = n.getChildByFieldName("variables");
		Node result_set = n.getChildByFieldName("result_set");
		Node body = n.getChildByFieldName("body");
		interpretForVariables(variables, level + 1, ctx);
		interpretIdentifier(result_set, level + 1, ctx);
		interpretForBody(body, level + 1, ctx);
	}

	void interpretForVariables(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("for_variables\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("identifier")) {
				interpretIdentifier(c, level + 1, ctx);
			}
		}
	}
	
	
	void interpretForBody(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("for_body\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("set")) {
				interpretSet(c, level + 1, ctx);
			} else if (c.getType().equals("execute_using")) {
				interpretExecuteUsing(c, level + 1, ctx);
			} if (c.getType().equals("execute_sql")) {
				interpretExecuteSql(c, level + 1, ctx);
			} if (c.getType().equals("siard_metadata")) {
				interpretSiardMetadata(c, level + 1, ctx);
			} if (c.getType().equals("siard_output")) {
				interpretSiardOutput(c, level + 1, ctx);
			} if (c.getType().equals("for_loop")) {
				interpretForLoop(c, level + 1, ctx);
			}
		}
	}
	
	void interpretVariableInstance(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("variable instance\n");
		for (Node c : n.getChildren()) {
			interpretIdentifier(c, level + 1, ctx);
		}
	}
	
	void interpretString(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("string\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("interpolation")) {
				interpretInterpolation(c, level + 1, ctx);
			} else if (c.getType().equals("escape_sequence")) {
				interpretEscapeSequence(c, level + 1, ctx);
			} else if (c.getType().equals("string_content")) {
				interpretStringContent(c, level + 1, ctx);
			}
		}
	}
	
	void interpretInterpolation(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("interpolation:\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("string")) {
				interpretString(c, level + 1, ctx);
			} else if (c.getType().equals("variable_instance")) {
				interpretVariableInstance(c, level + 1, ctx);
			}
		}
	}
	
	void interpretKeyValuePairs(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("key-value pairs\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("key_value_pair")) {
				interpretKeyValuePair(c, level + 1, ctx);
			} 
		}
	}

	void interpretKeyValuePair(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("key-value pair\n");
		Node key = n.getChildByFieldName("key");
		Node value = n.getChildByFieldName("value");
		interpretIdentifier(key, level + 1, ctx);
		if (value.getType().equals("raw")) {
			interpretRaw(value, level + 1, ctx);
		} else if (value.getType().equals("string")) {
			interpretString(value, level + 1, ctx);
		} else if (value.getType().equals("variable_instance")) {
			interpretVariableInstance(value, level + 1, ctx);
		} 
	}

	void interpretRaw(Node n, int level, Context ctx) {
		indent(level);
		System.out.format("Raw\n");
		for (Node c : n.getChildren()) {
			if (c.getType().equals("raw_content")) {
				interpretRawContent(c, level + 1, ctx);
			} else if (c.getType().equals("interpolation")) {
				interpretInterpolation(c, level + 1, ctx);
			}
		}
	}
	
	// Methods corresponding to terminals in grammar
	
	void interpretIdentifier(Node n, int level, Context ctx) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		indent(level);
		System.out.format("identifier: \"%s\"\n", contents);
	}
	
	void interpretShortDescription(Node n, int level, Context ctx) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		indent(level);
		System.out.format("short description: \"%s\"\n", contents);
	}
	
	void interpretEscapeSequence(Node n, int level, Context ctx) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		indent(level);
		System.out.format("escape sequence: \"%s\"\n", contents);
	}
	
	void interpretStringContent(Node n, int level, Context ctx) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		indent(level);
		System.out.format("string content: \"%s\"\n", contents);
	}
	
	void interpretRawContent(Node n, int level, Context ctx) {
		String contents = source.substring(n.getStartByte(), n.getEndByte());
		indent(level);
		System.out.format("raw content: \"%s\"\n", contents);
	}
	
}
