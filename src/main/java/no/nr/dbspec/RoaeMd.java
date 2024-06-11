package no.nr.dbspec;

public class RoaeMd extends MdBase<RoaeMd.RoaeMdType, RoaeMd> {
    public enum RoaeMdType {
        COMMAND, PARAMETER, SQL;
    }

    public RoaeMd(RoaeMdType type, String name, String data) {
        super(type, name, data);
    }
}
