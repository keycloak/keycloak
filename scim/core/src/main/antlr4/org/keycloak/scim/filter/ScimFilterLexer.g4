lexer grammar ScimFilterLexer;

// Logical operators (case-insensitive)
AND : [aA][nN][dD];
OR  : [oO][rR];
NOT : [nN][oO][tT];

// Comparison operators (case-insensitive)
EQ : [eE][qQ];
NE : [nN][eE];
CO : [cC][oO];
SW : [sS][wW];
EW : [eE][wW];
GT : [gG][tT];
GE : [gG][eE];
LT : [lL][tT];
LE : [lL][eE];
PR : [pP][rR];

// Grouping
LPAREN : '(';
RPAREN : ')';

// Literals
TRUE  : [tT][rR][uU][eE];
FALSE : [fF][aA][lL][sS][eE];
NULL  : [nN][uU][lL][lL];

// String literal (JSON string with escaping)
STRING : '"' ( ESC | SAFECODEPOINT )* '"';
fragment ESC : '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE : 'u' HEX HEX HEX HEX;
fragment HEX : [0-9a-fA-F];
fragment SAFECODEPOINT : ~["\\\u0000-\u001F];

// Number (JSON number format)
NUMBER : '-'? INT ('.' DIGIT+)? EXP?;
fragment INT : '0' | [1-9] DIGIT*;
fragment DIGIT : [0-9];
fragment EXP : [Ee] [+\-]? DIGIT+;

// Attribute path: [schema:]attributeName[.subAttribute]
// Supports full URN schema syntax (e.g., urn:ietf:params:scim:schemas:core:2.0:User:userName)
ATTRPATH : (SCHEMA_URI COLON)? ATTRNAME (DOT ATTRNAME)* (LBRACKET NUMBER RBRACKET DOT ATTRNAME)?;

// Schema URI: matches URN format like "urn:ietf:params:scim:schemas:core:2.0:User"
// or URL format like "http://example.com/schemas/User"
fragment SCHEMA_URI
    : URN_SCHEMA
    | URL_SCHEMA
    ;

// URN format: urn:ietf:params:scim:schemas:core:2.0:User
fragment URN_SCHEMA : 'urn' COLON [a-zA-Z0-9]+ (COLON [a-zA-Z0-9._-]+)+ ;

// URL format: http://example.com/schemas/User or https://example.com/schemas/User
fragment URL_SCHEMA : ('http' | 'https') COLON SLASHSLASH [a-zA-Z0-9:/.@#_-]+ ;

fragment ATTRNAME : ALPHA (ALPHA | DIGIT | '-' | '_')*;
fragment ALPHA : [a-zA-Z];
fragment DOT : '.';
fragment COLON : ':';
fragment SLASHSLASH : '//';
fragment LBRACKET : '[';
fragment RBRACKET : ']';

// Whitespace
WS : [ \t\r\n]+ -> skip;
