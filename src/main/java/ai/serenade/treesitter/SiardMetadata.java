package ai.serenade.treesitter;

import java.io.File;
import java.io.IOException;

import ai.serenade.treesitter.MdObject.MdType;
import ch.admin.bar.siard2.api.Archive;
import ch.admin.bar.siard2.api.MetaData;
import ch.admin.bar.siard2.api.primary.ArchiveImpl;

public class SiardMetadata {
  
  public static void updateMetadata(String siardFilename, MdObject mdo) {
	  Archive archive = ArchiveImpl.newInstance();
	  File siardFile = new File(siardFilename);
	  try {
		  archive.open(siardFile);
	      MetaData md = archive.getMetaData();
	      md.setDbName(getInfoField(mdo, "dbname"));
	      md.setDescription(getInfoField(mdo, "description"));
	      md.setArchiver(getInfoField(mdo, "archiver"));
	      md.setArchiverContact(getInfoField(mdo, "archiverContact"));
	      md.setDataOwner(getInfoField(mdo, "dataOwner"));
	      md.setDataOriginTimespan(getInfoField(mdo, "dataOriginTimespan"));
		  archive.close();
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
  }
  
  static String getInfoField(MdObject mdo, String name) {
	  MdObject child = mdo.getChild(MdType.INFO, name);
	  return child != null && child.getDocumentation().length() > 0 ? child.getDocumentation() : "<value not provided>";
  }
  
}
