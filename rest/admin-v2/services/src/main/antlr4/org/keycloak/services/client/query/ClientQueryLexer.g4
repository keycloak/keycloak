lexer grammar ClientQueryLexer;

COLON : ':';
DOT : '.';
LBRACKET : '[' -> pushMode(LIST_MODE);

QUOTED_STRING : '"' ~["]* '"';

BAREWORD : [a-zA-Z0-9_-]+;

WS : [ \t]+ -> skip;

mode LIST_MODE;
LIST_RBRACKET : ']' -> popMode;
LIST_COMMA : ',';
LIST_WS : [ \t]+ -> skip;
LIST_ENTRY : ~[ \t\],[]+;
