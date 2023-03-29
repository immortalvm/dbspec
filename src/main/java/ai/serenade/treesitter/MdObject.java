package ai.serenade.treesitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MdObject {
	
	public enum MdType {
		METADATA, INFO, SCHEMA,	TYPE, TABLE, COLUMN, FIELD, KEY, CHECK, VIEW;
	}

	MdType type;
	String name;
	String documentation;
	List<MdObject> children;
	
	public MdObject(MdType type, String name, String documentation) {
		this.type = type;
		this.name = name;
		this.documentation = documentation;
		this.children = new ArrayList<MdObject>();
	}
	
	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public void add(MdObject child) {
		this.children.add(child);
	}
	
	public String toString() {
		return String.format("(%s %s \"%s\"%S%s)", type, name, documentation.trim(), children.size() == 0 ? "" : " ",
				children.stream().map((x) -> x.toString()).collect(Collectors.joining(" "))); 
	}
	
}
