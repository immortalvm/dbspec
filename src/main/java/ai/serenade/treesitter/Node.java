package ai.serenade.treesitter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Node {
  private int context0;
  private int context1;
  private int context2;
  private int context3;
  private long id;
  private long tree;

  public Node() {}

  public Node getChild(int child) {
    return TreeSitter.nodeChild(this, child);
  }

  public int getChildCount() {
    return TreeSitter.nodeChildCount(this);
  }

  public boolean hasError() {
    return TreeSitter.nodeHasError(this);
  }

  public int getEndByte() {
    return TreeSitter.nodeEndByte(this);
  }

  public String getNodeString() {
    return TreeSitter.nodeString(this);
  }

  public int getStartByte() {
    return TreeSitter.nodeStartByte(this);
  }

  public String getType() {
	  return this.id != 0 ? TreeSitter.nodeType(this) : "";
  }

  public TreeCursor walk() {
    return new TreeCursor(TreeSitter.treeCursorNew(this));
  }
  
  public Node getChildByFieldName(String fieldName) {
	  byte[] bytes = fieldName.getBytes();
	  Node child = TreeSitter.nodeChildByFieldName(this, bytes, bytes.length);
	  return child.id != 0 ? child : null;
  }
  
  List<Node> getChildren() {
	  ArrayList<Node> list = new ArrayList<Node>();
	  for (int i = 0; i < getChildCount(); i++) {
		  list.add(getChild(i));
	  }
	  return list;
  }

}
