package ai.serenade.treesitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MdObject {
	
	public enum MdType {
		METADATA, DBNAME, DESCRIPTION, ARCHIVER, ARCHIVER_CONTACT, DATA_OWNER, DATA_ORIGIN_TIMESPAN, LOB_FOLDER, SCHEMA,
		TYPE, TABLE, COLUMN, FIELD, KEY, CHECK, VIEW;
	}

	MdType type;
	String documentation;
	List<MdObject> children;
	
	public MdObject(MdType type, String documentation) {
		this.type = type;
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
		return String.format("(%s \"%s\" %s)", type, documentation, children.stream().map((x) -> x.toString()).collect(Collectors.joining(" "))); 
	}
	
}
