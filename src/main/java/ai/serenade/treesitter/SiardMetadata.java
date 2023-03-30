package ai.serenade.treesitter;

import java.io.File;
import java.io.IOException;

import ai.serenade.treesitter.MdObject.MdType;
import ch.admin.bar.siard2.api.Archive;
import ch.admin.bar.siard2.api.MetaColumn;
import ch.admin.bar.siard2.api.MetaData;
import ch.admin.bar.siard2.api.MetaSchema;
import ch.admin.bar.siard2.api.MetaTable;
import ch.admin.bar.siard2.api.MetaView;
import ch.admin.bar.siard2.api.primary.ArchiveImpl;

public class SiardMetadata {
  
  public static void updateMetadata(String siardFilename, MdObject mdo) {
	  Archive archive = ArchiveImpl.newInstance();
	  File siardFile = new File(siardFilename);
	  try {
		  archive.open(siardFile);
	      MetaData md = archive.getMetaData();
	      updateMandatoryMetadata(md, mdo);
	      MetaSchema schema = archive.getSchema(0).getMetaSchema(); // FIXME: get schema by name
	      updateSchemaMetadata(schema, mdo);
		  archive.close();
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
  }
  
  static void updateMandatoryMetadata(MetaData md, MdObject mdo) {
	  md.setDbName(getInfoField(mdo, "dbname"));
	  md.setDescription(getInfoField(mdo, "description"));
	  md.setArchiver(getInfoField(mdo, "archiver"));
	  md.setArchiverContact(getInfoField(mdo, "archiverContact"));
	  md.setDataOwner(getInfoField(mdo, "dataOwner"));
	  md.setDataOriginTimespan(getInfoField(mdo, "dataOriginTimespan"));
  }
  
  static void updateSchemaMetadata(MetaSchema schema, MdObject mdo) {
	  MdObject schemaObject = mdo.getChild(MdType.SCHEMA);
	  if (schemaObject != null) {
		  String documentation = schemaObject.getDocumentation();
		  schemaObject.setDocumentation(documentation);
		  updateTableMetadata(schema, schemaObject);
		  updateViewMetadata(schema, schemaObject);
	  }
  }
  
  static void updateTableMetadata(MetaSchema schema, MdObject mdo) {
	  for (MdObject tObj : mdo.getChildren(MdType.TABLE)) {
		  String name = tObj.getName();
		  String documentation = tObj.getDocumentation();
		  System.out.format("*** Table: name='%s' documentation='%s'\n", name, documentation);
		  MetaTable table = schema.getMetaTable(name);
		  table.setDescription(documentation);
		  updateTableColumnMetadata(table, tObj);
	  }
  }
  
  static void updateTableColumnMetadata(MetaTable table, MdObject mdo) {
	  for (MdObject cObj : mdo.getChildren(MdType.COLUMN)) {
		  String name = cObj.getName();
		  String documentation = cObj.getDocumentation();
		  System.out.format("*** Table Column: name='%s' documentation='%s'\n", name, documentation);
		  MetaColumn column = table.getMetaColumn(name);
		  column.setDescription(documentation); 
	  }
  }
  
  static void updateViewMetadata(MetaSchema schema, MdObject mdo) {
	  for (MdObject vObj : mdo.getChildren(MdType.VIEW)) {
		  String name = vObj.getName();
		  String documentation = vObj.getDocumentation();
		  MetaView view = schema.getMetaView(name);
		  System.out.format("*** View: name='%s' documentation='%s'\n", name, documentation);
		  view.setDescription(documentation);
		  updateViewColumnMetadata(view, vObj);
	  }
  }
  
  static void updateViewColumnMetadata(MetaView view, MdObject mdo) {
	  for (MdObject cObj : mdo.getChildren(MdType.COLUMN)) {
		  String name = cObj.getName();
		  String documentation = cObj.getDocumentation();
		  System.out.format("*** View Column: name='%s' documentation='%s'\n", name, documentation);
		  MetaColumn column = view.getMetaColumn(name);
		  column.setDescription(documentation); 
	  }
  }
  
  static String getInfoField(MdObject mdo, String name) {
	  MdObject child = mdo.getChild(MdType.INFO, name);
	  return child != null && child.getDocumentation().length() > 0 ? child.getDocumentation() : "<value not provided>";
  }
  
}
