package ai.serenade.treesitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Siard {
	public static boolean VIEWS_AS_TABLES = false; // TODO: Does this make sense?
	public static long PROCESS_TIMEOUT_SECONDS = 10; 
	Dbms dbms;
	File jarDir;

	public Siard(Dbms dbms, File jarDir) {
		this.dbms = dbms;
		this.jarDir = jarDir;
	}
	
	public void transfer(Connection conn, String siardFilename, String lobFoldername) throws SiardError {
        try {
        	File siardJarFile = new File(new File(jarDir, "lib"), "siardcmd.jar");
    		String mimeType = ""; // TODO: what should be the value of mimeType
    		String jdbcUrl = conn.getMetaData().getURL().toString();
    		String dbUser = dbms.connectionParameters.get(conn).getProperty("user");
    		String dbPassword = dbms.connectionParameters.get(conn).getProperty("password");
    		List<String> cmd = new ArrayList<String>();
    		cmd.add("java");
    		cmd.add("-cp");
    		cmd.add(siardJarFile.getAbsolutePath());
    		cmd.add("ch.admin.bar.siard2.cmd.SiardFromDb");
    		cmd.add("-o");
    		if (VIEWS_AS_TABLES) {
    			cmd.add("-v");
    		}
    		if (lobFoldername != null) {
    			String canonicalLobFolderName = canonicalPath(lobFoldername);
    			cmd.add(String.format("-x=%s", canonicalLobFolderName));
    			//	cmd.add(String.format("-m=%s", mimeType));
    			File theDir = new File(canonicalLobFolderName);
    			if (!theDir.exists()){
    			    theDir.mkdirs();
    			}
    		}
    		cmd.add(String.format("-j=%s", jdbcUrl));
    		cmd.add(String.format("-u=%s", dbUser));
    		cmd.add(String.format("-p=%s", dbPassword));
    		cmd.add(String.format("-s=%s", canonicalPath(siardFilename)));
    		String[] cmdstrings = cmd.toArray(String[]::new);
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(cmdstrings);
            String response = readProcessOutput(p);
            p.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (p.exitValue() != 0) {
            	throw new SiardError(response);
            }
        } catch(Exception e) {
            throw new SiardError(e.toString());
        }
	}
	
    private String readProcessOutput(Process p) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String response = "";
        String line;
        while ((line = reader.readLine()) != null) {
            response += line + "\n";
        }
        reader.close();
        return response;
    }

    private String canonicalPath(String filename) throws IOException {
    	File f = new File(filename);
    	return f.getCanonicalPath();
    }
    
}
