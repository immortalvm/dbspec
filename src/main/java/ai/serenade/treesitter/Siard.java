package ai.serenade.treesitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Siard {
	public static boolean VIEWS_AS_TABLES = false; // TODO: Does this make sense?
	Dbms dbms;
	File jarDir;
	Log log;

	public Siard(Dbms dbms, File jarDir, Log log) {
		this.dbms = dbms;
		this.jarDir = jarDir;
        this.log = log;
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
            log.write(Log.DEBUG, "--- SIARD command: %s\n\n", String.join(" ", cmdstrings));
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(cmdstrings);
            p.waitFor();
            if (p.exitValue() != 0) {
            	throw new SiardError(Script.streamToString(p.getErrorStream())
                                     + Script.streamToString(p.getInputStream()));
            }
        } catch (SiardError e) {
            throw e;
        } catch(Exception e) {
            throw new SiardError(e.toString());
        }
	}

    private String canonicalPath(String filename) throws IOException {
    	File f = new File(filename);
    	return f.getCanonicalPath();
    }
    
}
