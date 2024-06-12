package no.nr.dbspec;

public enum StatusCode {
    OK(0),
    SPEC_UNREADABLE(1),
    SEMANTIC_ERROR(2),
    SQL_ERROR(3),
    SCRIPT_ERROR(4),
    ASSERTION_FAILURE(5),
    AST_ERROR(6),
    INTERNAL_ERROR(7),

    COULD_NOT_PARSE_OPTIONS(100),
    DIRECTORY_DOES_NOT_EXIST(101),
    DBSPEC_FILE_NOT_SPECIFIED(102),
    DBSPEC_FILE_NOT_FOUND(103),
    CONFIG_FILE_UNREADABLE(104), // It does, however, exist.
    ;

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
