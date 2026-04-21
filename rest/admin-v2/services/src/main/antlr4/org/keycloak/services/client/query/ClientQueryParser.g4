parser grammar ClientQueryParser;

options { tokenVocab = ClientQueryLexer; }

query : expression+ EOF;

expression : fieldPath COLON value;

fieldPath : BAREWORD (DOT BAREWORD)*;

value
    : BAREWORD          # BareValue
    | QUOTED_STRING     # QuotedValue
    | list              # ListValue
    ;

list : LBRACKET listEntry (LIST_COMMA listEntry)* LIST_RBRACKET;

listEntry : LIST_ENTRY;
