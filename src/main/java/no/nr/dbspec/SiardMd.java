package no.nr.dbspec;

public class SiardMd extends MdBase<SiardMd.SiardMdType, SiardMd> {

    public enum SiardMdType {
        METADATA, INFO, SCHEMA,    TYPE, TABLE, COLUMN, FIELD, KEY, CHECK, VIEW;
    }

    public SiardMd(SiardMdType type, String name, String documentation) {
        super(type, name, documentation);
    }
}
