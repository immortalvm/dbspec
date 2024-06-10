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
        File siardFile = new File(siardFilename);
        try {
            archive.open(siardFile);
            MetaData md = archive.getMetaData();
            updateDbLevelMetadata(md, mdo, connection, n);
            updateArchiveMetadata(archive, mdo);
            archive.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDbLevelMetadata(MetaData md, SiardMd mdo, Connection connection, TSNode n) {
        String x;
        x = getInfoField(mdo, "dbname");
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
            String name = sObj.getName();
            String documentation = sObj.getDocumentation();
            Schema schema = archive.getSchema(name);
            if (schema != null) {
                MetaSchema metaSchema = schema.getMetaSchema();
                metaSchema.setDescription(documentation);
                if (sObj != null) {
                    updateTableMetadata(metaSchema, sObj);
                    updateViewMetadata(metaSchema, sObj);
                    updateTypeMetadata(metaSchema, sObj);
                }
            }
        }
    }

    private void updateTableMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TABLE)) {
            String name = tObj.getName();
            String documentation = tObj.getDocumentation();
            MetaTable table = schema.getMetaTable(name);
            table.setDescription(documentation);
            if (table != null) {
                updateTableColumnMetadata(table, tObj);
                updateKeyMetadata(table, tObj);
                updateCheckMetadata(table, tObj);
            }
        }
    }

    private void updateTableColumnMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            String name = cObj.getName();
            String documentation = cObj.getDocumentation();
            MetaColumn column = table.getMetaColumn(name);
            if (column != null) {
                column.setDescription(documentation);
                updateFieldMetadata(column, cObj);
            }
        }
    }

    private void updateViewMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd vObj : mdo.getChildren(SiardMdType.VIEW)) {
            String name = vObj.getName();
            String documentation = vObj.getDocumentation();
            MetaView view = schema.getMetaView(name);
            view.setDescription(documentation);
            if (view != null) {
                updateViewColumnMetadata(view, vObj);
            }
        }
    }

    private void updateViewColumnMetadata(MetaView view, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            String name = cObj.getName();
            String documentation = cObj.getDocumentation();
            MetaColumn column = view.getMetaColumn(name);
            if (column != null) {
                column.setDescription(documentation);
                updateFieldMetadata(column, cObj);
            }
        }
    }

    private void updateTypeMetadata(MetaSchema schema, SiardMd mdo) {
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TYPE)) {
            String name = tObj.getName();
            String documentation = tObj.getDocumentation();
            MetaType type = schema.getMetaType(name);
            if (type != null) {
                type.setDescription(documentation);
            }
        }
    }

    private void updateFieldMetadata(MetaColumn column, SiardMd mdo) {
        for (SiardMd fObj : mdo.getChildren(SiardMdType.FIELD)) {
            String name = fObj.getName();
            String documentation = fObj.getDocumentation();
            MetaField field;
            try {
                field = column.getMetaField(name);
                if (field != null) {
                    field.setDescription(documentation);
                }
            } catch (IOException e) {
            }
        }
    }

    private void updateKeyMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd kObj : mdo.getChildren(SiardMdType.KEY)) {
            String name = kObj.getName();
            String documentation = kObj.getDocumentation();
            MetaForeignKey key = table.getMetaForeignKey(documentation);
            if (key != null) {
                key.setDescription(documentation);
            }
        }
    }

    private void updateCheckMetadata(MetaTable table, SiardMd mdo) {
        for (SiardMd cObj : mdo.getChildren(SiardMdType.KEY)) {
            String name = cObj.getName();
            String documentation = cObj.getDocumentation();
            MetaCheckConstraint check = table.getMetaCheckConstraint(name);
            if (check != null) {
                check.setDescription(documentation);
            }
        }
    }

    private String getInfoField(SiardMd mdo, String name) {
        SiardMd child = mdo.getChild(SiardMdType.INFO, name);
        return child != null && child.getDocumentation().length() > 0 ? child.getDocumentation() : null;
    }
}
