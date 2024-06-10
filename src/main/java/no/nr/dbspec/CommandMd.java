package no.nr.dbspec;

public class CommandMd extends MdBase<CommandMd.CommandMdType, CommandMd> {
    public enum CommandMdType {
        COMMAND, PARAMETER, SQL;
    }

    public CommandMd(CommandMdType type, String name, String documentation) {
        super(type, name, documentation);
    }
}
