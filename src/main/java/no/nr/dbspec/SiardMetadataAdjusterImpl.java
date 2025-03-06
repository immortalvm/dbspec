package no.nr.dbspec;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import no.nr.dbspec.SiardMd.SiardMdType;
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

public class SiardMetadataAdjusterImpl implements SiardMetadataAdjuster {

    private final Log log;

    public SiardMetadataAdjusterImpl(Log log) {
        this.log = log;
    }

    @Override
    public void updateMetadata(String siardFilename, SiardMd mdo, Connection connection) throws SiardException {
        Archive archive = ArchiveImpl.newInstance();
        try {
            File siardFile = new File(siardFilename);
            try {
                archive.open(siardFile);
                MetaData md = archive.getMetaData();
                updateDbLevelMetadata(md, mdo, connection);
                updateArchiveMetadata(archive, mdo);
            } finally {
                archive.close();
            }
        } catch (IOException e) {
            log.maybePrintStackTrace(e);
            throw new SiardException(e);
        }
    }

    private void updateDbLevelMetadata(MetaData md, SiardMd mdo, Connection connection) {
        String x = getInfoField(mdo, "dbname");
        if (x == null) {
            try {
                x = connection.getCatalog();
            } catch (SQLException e) {
                throw new SiardException(e.getMessage());
            }
        }
        if (x != null) md.setDbName(x);
        if (null != (x = getInfoField(mdo, "description"))) md.setDescription(x);
        if (null != (x = getInfoField(mdo, "archiver"))) md.setArchiver(x);
        if (null != (x = getInfoField(mdo, "archiverContact"))) md.setArchiverContact(x);
        if (null != (x = getInfoField(mdo, "dataOwner"))) md.setDataOwner(x);
        if (null != (x = getInfoField(mdo, "dataOriginTimespan"))) md.setDataOriginTimespan(x);
    }

    private void updateArchiveMetadata(Archive archive, SiardMd mdo) {
        for (SiardMd sObj : mdo.getChildren(SiardMdType.SCHEMA)) {
            String name = sObj.getName();
            Schema schema =
                    ensureNotNull("Schema", "", name, archive::getSchema);
            MetaSchema metaSchema =
                    ensureNotNull("Schema metadata", "", name, _n -> schema.getMetaSchema());
            metaSchema.setDescription(sObj.getData());
            String prefix = name + ".";
            updateTableMetadata(metaSchema, sObj, prefix);
            updateViewMetadata(metaSchema, sObj, prefix);
            updateTypeMetadata(metaSchema, sObj, prefix);
        }
    }

    private void updateTableMetadata(MetaSchema schema, SiardMd mdo, String prefix) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TABLE)) {
            String name = tObj.getName();
            MetaTable table = ensureNotNull("Table", prefix, name, schema::getMetaTable);
            table.setDescription(tObj.getData());
            String p2 = prefix + name + ".";
            updateTableColumnMetadata(table, tObj, p2);
            updateKeyMetadata(table, tObj, p2);
            updateCheckMetadata(table, tObj, p2);
        }
    }

    private void updateTableColumnMetadata(MetaTable table, SiardMd mdo, String prefix) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            String name = cObj.getName();
            MetaColumn column =
                    ensureNotNull("Column of " + table.getName(), prefix, name, table::getMetaColumn);
            column.setDescription(cObj.getData());
            updateFieldMetadata(column, cObj, prefix + name + ".");
        }
    }

    private void updateViewMetadata(MetaSchema schema, SiardMd mdo, String prefix) {
        for (SiardMd vObj : mdo.getChildren(SiardMdType.VIEW)) {
            String name = vObj.getName();
            MetaView view = ensureNotNull("View", prefix, name, schema::getMetaView);
            view.setDescription(vObj.getData());
            String p2 = prefix + name + ".";
            updateViewColumnMetadata(view, vObj, p2);
        }
    }

    private void updateViewColumnMetadata(MetaView view, SiardMd mdo, String prefix) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            String name = cObj.getName();
            MetaColumn column = ensureNotNull("View column", prefix, name, view::getMetaColumn);
            column.setDescription(cObj.getData());
            updateFieldMetadata(column, cObj, prefix + name + ".");
        }
    }

    private void updateTypeMetadata(MetaSchema schema, SiardMd mdo, String prefix) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TYPE)) {
            MetaType type = ensureNotNull("Type", prefix, tObj.getName(), schema::getMetaType);
            type.setDescription(tObj.getData());
        }
    }

    private void updateFieldMetadata(MetaColumn column, SiardMd mdo, String prefix) {
        for (SiardMd fObj : mdo.getChildren(SiardMdType.FIELD)) {
            MetaField field = ensureNotNull("Column field", prefix, fObj.getName(), n -> {
                try {
                    return column.getMetaField(n);
                } catch (IOException e) {
                    log.maybePrintStackTrace(e);
                    throw new SiardException(e);
                }
            });
            field.setDescription(fObj.getData());
        }
    }

    private void updateKeyMetadata(MetaTable table, SiardMd mdo, String prefix) {
        for (SiardMd kObj : mdo.getChildren(SiardMdType.KEY)) {
            MetaForeignKey key = ensureNotNull("Foreign key", prefix, kObj.getName(), table::getMetaForeignKey);
            key.setDescription(kObj.getData());
        }
    }

    private void updateCheckMetadata(MetaTable table, SiardMd mdo, String prefix) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.KEY)) {
            MetaCheckConstraint check = ensureNotNull("Check", prefix, cObj.getName(), table::getMetaCheckConstraint);
            check.setDescription(cObj.getData());
        }
    }

    private static <T> T ensureNotNull(String kind, String prefix, String name, Function<String, T> getter) {
        T res = getter.apply(name);
        if (res == null) {
            throw new SiardException(kind + " not found: " + prefix + name);
        }
        return res;
    }

    private String getInfoField(SiardMd mdo, String name) {
        SiardMd child = mdo.getChild(SiardMdType.INFO, name);
        return child == null ? null : child.getData();
    }
}
