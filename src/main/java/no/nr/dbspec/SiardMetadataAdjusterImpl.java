package no.nr.dbspec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import static no.nr.dbspec.Utils.pluralS;

public class SiardMetadataAdjusterImpl implements SiardMetadataAdjuster {

    private final Log log;

    public SiardMetadataAdjusterImpl(Log log) {
        this.log = log;
    }

    @Override
    public void updateMetadata(Path siardFilePath, SiardMd mdo, Connection connection) throws SiardException {
        Archive archive = ArchiveImpl.newInstance();
        try {
            File siardFile = siardFilePath.toFile();
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
        HashSet<String> schemas = new HashSet<>();
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
            schemas.add(name);
        }
        int nMissing = archive.getSchemas() - schemas.size();
        if (nMissing > 0) {
            String missing = IntStream
                    .range(0, archive.getSchemas())
                    .mapToObj(archive::getSchema)
                    .map(Schema::getMetaSchema)
                    .map(MetaSchema::getName)
                    .filter(Predicate.not(schemas::contains))
                    .collect(Collectors.joining(", "));
            log.warn("Schema%s in SIARD file not mentioned in Metadata: %s", pluralS(nMissing), missing);
        }
    }

    private void updateTableMetadata(MetaSchema schema, SiardMd mdo, String prefix) {
        HashSet<String> tables = new HashSet<>();
        for (SiardMd tObj : mdo.getChildren(SiardMdType.TABLE)) {
            String name = tObj.getName();
            MetaTable table = ensureNotNull("Table", prefix, name, schema::getMetaTable);
            table.setDescription(tObj.getData());
            String p2 = prefix + name + ".";
            updateTableColumnMetadata(table, tObj, p2);
            updateKeyMetadata(table, tObj, p2);
            updateCheckMetadata(table, tObj, p2);
            tables.add(name);
        }
        int nMissing = schema.getMetaTables()- tables.size();
        if (nMissing > 0) {
            String missing = IntStream
                    .range(0, schema.getMetaTables())
                    .mapToObj(schema::getMetaTable)
                    .map(MetaTable::getName)
                    .filter(Predicate.not(tables::contains))
                    .collect(Collectors.joining(", "));
            log.warn("Table%s of schema '%s' in SIARD file not mentioned in Metadata: %s",
                    pluralS(nMissing), stripTrailingDots(prefix), missing);
        }
    }

    private void updateTableColumnMetadata(MetaTable table, SiardMd mdo, String prefix) {
        HashSet<String> columns = new HashSet<>();
        for (SiardMd cObj : mdo.getChildren(SiardMdType.COLUMN)) {
            String name = cObj.getName();
            MetaColumn column =
                    ensureNotNull("Column of " + table.getName(), prefix, name, table::getMetaColumn);
            column.setDescription(cObj.getData());
            updateFieldMetadata(column, cObj, prefix + name + ".");
            columns.add(name);
        }
        int nMissing = table.getMetaColumns() - columns.size();
        if (nMissing > 0) {
            String missing = IntStream
                    .range(0, table.getMetaColumns())
                    .mapToObj(table::getMetaColumn)
                    .map(MetaColumn::getName)
                    .filter(Predicate.not(columns::contains))
                    .collect(Collectors.joining(", "));
            log.warn("Column%s of table '%s' in SIARD file not mentioned in Metadata: %s",
                    pluralS(nMissing), stripTrailingDots(prefix), missing);
        }
    }

    private void updateViewMetadata(MetaSchema schema, SiardMd mdo, String prefix) {
        HashSet<String> views = new HashSet<>();
        for (SiardMd vObj : mdo.getChildren(SiardMdType.VIEW)) {
            String name = vObj.getName();
            MetaView view = ensureNotNull("View", prefix, name, schema::getMetaView);
            view.setDescription(vObj.getData());
            String p2 = prefix + name + ".";
            updateViewColumnMetadata(view, vObj, p2);
            if (view.getQueryOriginal() == null) {
                // Since SIARD Suite does not extract the original queries for PostgreSQL (as of March 2025).
                log.warn("Original query missing for view '%s%s'.", prefix, name);
            }
            views.add(name);
        }
        int nMissing = schema.getMetaViews() - views.size();
        if (nMissing > 0) {
            String missing = IntStream
                    .range(0, schema.getMetaViews())
                    .mapToObj(schema::getMetaView)
                    .map(MetaView::getName)
                    .filter(Predicate.not(views::contains))
                    .collect(Collectors.joining(", "));
            log.warn("View%s of schema '%s' in SIARD file not mentioned in Metadata: %s",
                    pluralS(nMissing), stripTrailingDots(prefix), missing);
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

    private static String stripTrailingDots(String prefix) {
        int n = prefix.length();
        while (n > 0 && prefix.charAt(n -1) == '.') {
            n--;
        }
        return prefix.substring(0, n);
    }
}
