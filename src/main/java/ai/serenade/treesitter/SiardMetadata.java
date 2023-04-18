package ai.serenade.treesitter;

import java.io.File;
import java.io.IOException;

import ai.serenade.treesitter.MdObject.MdType;
import ch.admin.bar.siard2.api.Archive;
import ch.admin.bar.siard2.api.MetaCheckConstraint;
import ch.admin.bar.siard2.api.MetaColumn;
import ch.admin.bar.siard2.api.MetaData;
import ch.admin.bar.siard2.api.MetaField;
import ch.admin.bar.siard2.api.MetaForeignKey;
import ch.admin.bar.siard2.api.MetaSchema;
import ch.admin.bar.siard2.api.MetaTable;
import ch.admin.bar.siard2.api.MetaType;
import ch.admin.bar.siard2.api.MetaView;
import ch.admin.bar.siard2.api.Schema;
import ch.admin.bar.siard2.api.primary.ArchiveImpl;

public class SiardMetadata {
  
  public static void updateMetadata(String siardFilename, MdObject mdo) {
	  Archive archive = ArchiveImpl.newInstance();
	  File siardFile = new File(siardFilename);
	  try {
		  archive.open(siardFile);
	      MetaData md = archive.getMetaData();
	      updateMandatoryMetadata(md, mdo);
	      updateArchiveMetadata(archive, mdo);
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
  
  static void updateArchiveMetadata(Archive archive, MdObject mdo) {
	  for (MdObject sObj : mdo.getChildren(MdType.SCHEMA)) {
		  String name = sObj.getName();
		  String documentation = sObj.getDocumentation();
		  Schema schema = archive.getSchema(name);
		  if (schema != null) {
			  MetaSchema metaSchema = schema.getMetaSchema();
			  metaSchema.setDescription(documentation);
			  if (sObj != null) {
				  updateTableMetadata(metaSchema, sObj);
				  updateViewMetadata(metaSchema, sObj);
				  updateTypeMetadata(metaSchema, sObj);
			  }
		  }
	  }
  }
  
  static void updateTableMetadata(MetaSchema schema, MdObject mdo) {
	  for (MdObject tObj : mdo.getChildren(MdType.TABLE)) {
		  String name = tObj.getName();
		  String documentation = tObj.getDocumentation();
		  MetaTable table = schema.getMetaTable(name);
		  table.setDescription(documentation);
		  if (table != null) {
			  updateTableColumnMetadata(table, tObj);
			  updateKeyMetadata(table, tObj);
			  updateCheckMetadata(table, tObj);
		  }
	  }
  }
  
  static void updateTableColumnMetadata(MetaTable table, MdObject mdo) {
	  for (MdObject cObj : mdo.getChildren(MdType.COLUMN)) {
		  String name = cObj.getName();
		  String documentation = cObj.getDocumentation();
		  MetaColumn column = table.getMetaColumn(name);
		  if (column != null) {
			  column.setDescription(documentation);
			  updateFieldMetadata(column, cObj);
		  }
	  }
  }

  static void updateViewMetadata(MetaSchema schema, MdObject mdo) {
	  for (MdObject vObj : mdo.getChildren(MdType.VIEW)) {
		  String name = vObj.getName();
		  String documentation = vObj.getDocumentation();
		  MetaView view = schema.getMetaView(name);
		  view.setDescription(documentation);
		  if (view != null) {
			  updateViewColumnMetadata(view, vObj);
		  }
	  }
  }
  
  static void updateViewColumnMetadata(MetaView view, MdObject mdo) {
	  for (MdObject cObj : mdo.getChildren(MdType.COLUMN)) {
		  String name = cObj.getName();
		  String documentation = cObj.getDocumentation();
		  MetaColumn column = view.getMetaColumn(name);
		  if (column != null) {
			  column.setDescription(documentation); 
			  updateFieldMetadata(column, cObj);
		  }
	  }
  }
  
  static void updateTypeMetadata(MetaSchema schema, MdObject mdo) {
	  for (MdObject tObj : mdo.getChildren(MdType.TYPE)) {
		  String name = tObj.getName();
		  String documentation = tObj.getDocumentation();
		  MetaType type = schema.getMetaType(name);
		  if (type != null) {
			  type.setDescription(documentation);
		  }
	  }
  }

  static void updateFieldMetadata(MetaColumn column, MdObject mdo) {
	  for (MdObject fObj : mdo.getChildren(MdType.FIELD)) {
		  String name = fObj.getName();
		  String documentation = fObj.getDocumentation();
		  MetaField field;
		try {
			field = column.getMetaField(name);
			if (field != null) {
				field.setDescription(documentation);
			}
		} catch (IOException e) {
		}
	  }
  }

  static void updateKeyMetadata(MetaTable table, MdObject mdo) {
	  for (MdObject kObj : mdo.getChildren(MdType.KEY)) {
		  String name = kObj.getName();
		  String documentation = kObj.getDocumentation();
		  MetaForeignKey key = table.getMetaForeignKey(documentation);
		  if (key != null) {
			  key.setDescription(documentation);
		  }
	  }
  }
  
  static void updateCheckMetadata(MetaTable table, MdObject mdo) {
	  for (MdObject cObj : mdo.getChildren(MdType.KEY)) {
		  String name = cObj.getName();
		  String documentation = cObj.getDocumentation();
		  MetaCheckConstraint check = table.getMetaCheckConstraint(name);
		  if (check != null) {
			  check.setDescription(documentation);
		  }
	  }
  }
  
  static String getInfoField(MdObject mdo, String name) {
	  MdObject child = mdo.getChild(MdType.INFO, name);
	  return child != null && child.getDocumentation().length() > 0 ? child.getDocumentation() : "<value not provided>";
  }
  
}
