package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each argument type. There are two constants for each argument type, as a byte or as a
 * String (the _STRING version)
 *
 * @since 5.0.0 - 2023-10-23
 */
public final class ArgumentType {
    public static final String BYTE_STRING           = "y";
    public static final String BOOLEAN_STRING        = "b";
    public static final String INT16_STRING          = "n";
    public static final String UINT16_STRING         = "q";
    public static final String INT32_STRING          = "i";
    public static final String UINT32_STRING         = "u";
    public static final String INT64_STRING          = "x";
    public static final String UINT64_STRING         = "t";
    public static final String DOUBLE_STRING         = "d";
    public static final String FLOAT_STRING          = "f";
    public static final String STRING_STRING         = "s";
    public static final String OBJECT_PATH_STRING    = "o";
    public static final String SIGNATURE_STRING      = "g";
    public static final String FILEDESCRIPTOR_STRING = "h";
    public static final String ARRAY_STRING          = "a";
    public static final String VARIANT_STRING        = "v";
    public static final String STRUCT_STRING         = "r";
    public static final String STRUCT1_STRING        = "(";
    public static final String STRUCT2_STRING        = ")";
    public static final String DICT_ENTRY_STRING     = "e";
    public static final String DICT_ENTRY1_STRING    = "{";
    public static final String DICT_ENTRY2_STRING    = "}";

    public static final byte   BYTE                  = 'y';
    public static final byte   BOOLEAN               = 'b';
    public static final byte   INT16                 = 'n';
    public static final byte   UINT16                = 'q';
    public static final byte   INT32                 = 'i';
    public static final byte   UINT32                = 'u';
    public static final byte   INT64                 = 'x';
    public static final byte   UINT64                = 't';
    public static final byte   DOUBLE                = 'd';
    public static final byte   FLOAT                 = 'f';
    public static final byte   STRING                = 's';
    public static final byte   OBJECT_PATH           = 'o';
    public static final byte   SIGNATURE             = 'g';
    public static final byte   FILEDESCRIPTOR        = 'h';
    public static final byte   ARRAY                 = 'a';
    public static final byte   VARIANT               = 'v';
    public static final byte   STRUCT                = 'r';
    public static final byte   STRUCT1               = '(';
    public static final byte   STRUCT2               = ')';
    public static final byte   DICT_ENTRY            = 'e';
    public static final byte   DICT_ENTRY1           = '{';
    public static final byte   DICT_ENTRY2           = '}';

    private ArgumentType() {

    }
}
