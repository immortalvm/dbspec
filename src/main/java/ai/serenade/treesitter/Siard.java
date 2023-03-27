package ai.serenade.treesitter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import ch.admin.bar.siard2.api.Archive;
import ch.admin.bar.siard2.api.MetaColumn;
import ch.admin.bar.siard2.api.MetaSchema;
import ch.admin.bar.siard2.api.MetaTable;
import ch.admin.bar.siard2.api.primary.ArchiveImpl;
import ch.admin.bar.siard2.cmd.MetaDataFromDb;
import ch.admin.bar.siard2.cmd.PrimaryDataFromDb;
import ch.enterag.utils.ProgramInfo;

public class Siard {
	public static boolean VIEWS_AS_TABLES = false; // TODO: Does this make sense?
	// This is needed to prevent a RuntimeException in the SIARD code
	private static ProgramInfo pi = ProgramInfo.getProgramInfo(
			"", "", "", "", "",	"",
			Arrays.asList((new String[] {})),
			Arrays.asList((new String[] {})),
			Arrays.asList((new String[] {})),
			Arrays.asList((new String[] {})));

	public static boolean transfer(Connection conn, String siardFilename, String lobFoldername) {
		String lobMimeType = null; // TODO: should this be an argument?
		URI lobFolderUri = null;

		try {
			System.out.format("Connected to %s\n", conn.getMetaData().getURL().toString());
			conn.setAutoCommit(false);
			File siardFile = new File(siardFilename);
			if (siardFile.exists()) {
				siardFile.delete();
			}
			Archive archive = ArchiveImpl.newInstance();
			archive.create(siardFile);
			/* get meta data from DB */
			File lobFolder = new File(lobFoldername);
			if (!(lobFolder.exists() && lobFolder.isDirectory())) {
				return false;
			}
			MetaDataFromDb mdfd = MetaDataFromDb.newInstance(conn.getMetaData(), archive.getMetaData());
			mdfd.download(VIEWS_AS_TABLES, (lobFolderUri != null), null);
			/* set external LOB stuff */
			File relativePath = siardFile.getAbsoluteFile().getParentFile().toPath().relativize(lobFolder.getAbsoluteFile().toPath()).toFile();
			try {
				lobFolderUri = new URI("../" + relativePath.toString() + "/");
			} catch (URISyntaxException e) {
			}
			if (lobFolderUri != null) {
				MetaColumn maxLob = mdfd.getMaxLobColumn();
				if (maxLob != null) {
					String columnName = maxLob.getName();
					MetaTable mtLob = maxLob.getParentMetaTable();
					String tableName = mtLob.getName();
					MetaSchema msLob = mtLob.getParentMetaSchema();
					String schemaName = msLob.getName();
					String message = String.format("LOBs in database column '%s' in table '%s' in schema '%s' will be stored externally in folder '%s'",
							columnName, tableName, schemaName, lobFolder.getAbsolutePath().toString());
					maxLob.setLobFolder(lobFolderUri);
					if (lobMimeType != null) {
						maxLob.setMimeType(lobMimeType);
						message = message + " with MIME type " + maxLob.getMimeType();
					}
					System.out.println(message);
				} else {
					System.out.println("No LOB column found to be externalized!");
				}
			}
			PrimaryDataFromDb pdfd = PrimaryDataFromDb.newInstance(conn, archive);
			pdfd.download(null);
			/* close SIARD archive */
			archive.close();
			conn.rollback();
			return true;

		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

}
