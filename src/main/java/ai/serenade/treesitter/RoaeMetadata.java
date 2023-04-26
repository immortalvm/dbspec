package ai.serenade.treesitter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import ai.serenade.treesitter.MdObject.MdType;

public class RoaeMetadata {
	
	  public static void updateMetadata(String roaeFilename, MdObject mdo) {
		  try {
			  PrintWriter output = new PrintWriter(new FileWriter(roaeFilename));
			  for (MdObject cObj : mdo.getChildren(MdType.COMMAND)) {
				  output.write(cObj.getDocumentation());
				  output.format("%s\n", cObj.getDocumentation());
				  for (MdObject pObj : cObj.getChildren(MdType.PARAMETER)) {
					  output.format("%s - %s\n", pObj.getName(), pObj.getDocumentation());
				  }
				  for (MdObject sObj : cObj.getChildren(MdType.SQL)) {
					  output.format("%s\n", sObj.getDocumentation());
				  }
			  }
			  output.close();
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
	  }

}
