package no.nr.dbspec;

import ch.admin.bar.siard2.cmd.SiardFromDb;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;

public class Siard {
    private static final String SIARD_CMD_DRIVERS_PROPERTY = "ch.admin.bar.siard2.cmd.drivers";
    private static final String SIARD_CMD_LOG_PROPERTY = "java.util.logging.config.file";
    private final Dbms dbms;
    private final Log log;
    private final Path dir;

    public Siard(Dbms dbms, Log log, Path dir) {
        this.dbms = dbms;
        this.log = log;
        this.dir = dir;
    }

    private List<String> fileProperty(String property, String resourceName) throws URISyntaxException {
        String value = System.getProperty(property);
        if (value == null) {
            URL url = getClass().getResource(resourceName);
            assert url != null;
            if (url.toURI().getScheme().equals("file")) {
                value = url.getPath();
            }
        }
        return value == null ? Collections.emptyList()
                : Collections.singletonList("-D" + property + "=" + value);
    }

    public void transfer(Connection conn, String siardFilename, Log log) throws SiardError {
        try {
            String jdbcUrl = conn.getMetaData().getURL();
            String dbUser = dbms.connectionParameters.get(conn).getProperty("user");
            String dbPassword = dbms.connectionParameters.get(conn).getProperty("password");
            List<String> cmd = new ArrayList<String>();

            // https://stackoverflow.com/a/61860951
            cmd.add(ProcessHandle.current().info().command().orElseThrow());
            cmd.add("-cp");
            cmd.add(System.getProperty("java.class.path"));

            // Compatibility with JAVA 17 and later, cf. https://github.com/keeps/dbptk-developer
            cmd.add("--add-opens"); cmd.add("java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED");
            cmd.add("--add-opens"); cmd.add("java.xml/com.sun.org.apache.xalan.internal.xsltc.trax=ALL-UNNAMED");
            cmd.add("-Dfile.encoding=UTF-8");

            // SiardCmd config files
            cmd.addAll(fileProperty(SIARD_CMD_DRIVERS_PROPERTY, "/etc/jdbcdrivers.properties"));
            cmd.addAll(fileProperty(SIARD_CMD_LOG_PROPERTY, "/etc/logging.properties"));

            cmd.add(SiardFromDb.class.getCanonicalName()); // Main class

            // SiardFromDb options
            cmd.add("-o");
            cmd.add(String.format("-j=%s", jdbcUrl));
            cmd.add(String.format("-u=%s", dbUser));
            cmd.add(String.format("-p=%s", dbPassword));
            cmd.add(String.format("-s=%s", dir.resolve(siardFilename).toFile().getCanonicalPath()));
            log.write(Log.DEBUG, "--- Siard command: %s\n\n", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .directory(dir.toFile());
            boolean showOutput = log.getLevel() >= Log.DEBUG;
            if (showOutput) {
                pb.inheritIO();
            }
            Process p = pb.start();
            p.waitFor();
            if (p.exitValue() != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Exit value ").append(p.exitValue()).append(".");
                if (!showOutput) {
                    String ls = System.lineSeparator();
                    String dashes = "-".repeat(72);
                    sb.append(" Output:").append(ls).append(dashes).append(ls);
                    sb.append(Script.streamToString(p.getInputStream()));
                    sb.append(ls).append(dashes);
                }
                throw new SiardError(sb.toString());
            }

        } catch (SiardError e) {
            throw e;
        } catch(Exception e) {
            log.maybePrintStackTrace(e);
            throw new SiardError(e.toString() + System.lineSeparator());
        }
    }
}
