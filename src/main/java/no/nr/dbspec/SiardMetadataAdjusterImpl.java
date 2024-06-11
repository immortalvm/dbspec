package no.nr.dbspec;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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
import org.treesitter.TSNode;

public class SiardMetadataAdjusterImpl implements SiardMetadataAdjuster {

    @Override
    public void updateMetadata(String siardFilename, SiardMd mdo, Connection connection, TSNode n) {
        Archive archive = ArchiveImpl.newInstance();
        try {
            File siardFile = new File(siardFilename);
            try {
                archive.open(siardFile);
                MetaData md = archive.getMetaData();
                updateDbLevelMetadata(md, mdo, connection, n);
                updateArchiveMetadata(archive, mdo);
            } finally {
                archive.close();
            }
        } catch (IOException e) {
            // TODO: Log and propagate exception
            e.printStackTrace();
        }
    }

    private void updateDbLevelMetadata(MetaData md, SiardMd mdo, Connection connection, TSNode n) {
        String x = getInfoField(mdo, "dbname");
        if (x == null) {
            try {
                x = connection.getCatalog();
            } catch (SQLException e) {
                throw new SqlError(n, e.getMessage());
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
            Schema schema = archive.getSchema(sObj.getName());
            if (schema != null) {
                MetaSchema metaSchema = schema.getMetaSchema();
                if (metaSchema != null) {
                    metaSchema.setDescription(sObj.getData());
                    updateTableMetadata(metaSchema, sObj);
                    updateViewMetadata(metaSchema, sObj);
                    updateTypeMetadata(metaSchema, sObj);
                }
            }
        }
    }

    private void updateTableMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TABLE)) {
            MetaTable table = schema.getMetaTable(tObj.getName());
            if (table != null) {
                table.setDescription(tObj.getData());
                updateTableColumnMetadata(table, tObj);
                updateKeyMetadata(table, tObj);
                updateCheckMetadata(table, tObj);
            }
        }
    }

    private void updateTableColumnMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            MetaColumn column = table.getMetaColumn(cObj.getName());
            if (column != null) {
                column.setDescription(cObj.getData());
                updateFieldMetadata(column, cObj);
            }
        }
    }

    private void updateViewMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd vObj : mdo.getChildren(SiardMdType.VIEW)) {
            MetaView view = schema.getMetaView(vObj.getName());
            if (view != null) {
                view.setDescription(vObj.getData());
                updateViewColumnMetadata(view, vObj);
            }
        }
    }

    private void updateViewColumnMetadata(MetaView view, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            MetaColumn column = view.getMetaColumn(cObj.getName());
            if (column != null) {
                column.setDescription(cObj.getData());
                updateFieldMetadata(column, cObj);
            }
        }
    }

    private void updateTypeMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TYPE)) {
            MetaType type = schema.getMetaType(tObj.getName());
            if (type != null) {
                type.setDescription(tObj.getData());
            }
        }
    }

    private void updateFieldMetadata(MetaColumn column, SiardMd mdo) {
        for (SiardMd fObj : mdo.getChildren(SiardMdType.FIELD)) {
            try {
                MetaField field = column.getMetaField(fObj.getName());
                if (field != null) {
                    field.setDescription(fObj.getData());
                }
            } catch (IOException e) {
                // TODO: This should at least be logged.
            }
        }
    }

    private void updateKeyMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd kObj : mdo.getChildren(SiardMdType.KEY)) {
            MetaForeignKey key = table.getMetaForeignKey(kObj.getName());
            if (key != null) {
                key.setDescription(kObj.getData());
            }
        }
    }

    private void updateCheckMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.KEY)) {
            MetaCheckConstraint check = table.getMetaCheckConstraint(cObj.getName());
            if (check != null) {
                check.setDescription(cObj.getData());
            }
        }
    }

    private String getInfoField(SiardMd mdo, String name) {
        SiardMd child = mdo.getChild(SiardMdType.INFO, name);
        return child == null ? null : child.getData();
    }
}
