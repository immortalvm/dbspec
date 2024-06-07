package no.nr.dbspec;

import org.treesitter.TSNode;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class ScriptRunnerFake implements ScriptRunner {

    private final Database database;

    public ScriptRunnerFake(Database database) {
        this.database = database;
    }

    @Override
    public String execute(TSNode n, String interpreter, String script, Path dir) {
        int id = database.trace(Map.of(
                "type", "script",
                "interpreter", interpreter,
                "script", script,
                "path", dir.toString()));
        return Integer.toString(id);
    }
}
