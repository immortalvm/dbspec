package no.nr.dbspec;

import org.treesitter.TSNode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;

public interface ScriptRunner {
    String execute(TSNode n, String interpreter, String script, Path dir);
}
