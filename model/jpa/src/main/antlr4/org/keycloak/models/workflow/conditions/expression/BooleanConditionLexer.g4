lexer grammar BooleanConditionLexer;

// --- DEFAULT_MODE (mode 0) ---
OR  : 'OR';
AND : 'AND';
NOT : '!';

Identifier : [\p{L}_][\p{L}0-9_/-]*;

LPAREN : '(' ; // For (A OR B) expressions
RPAREN : ')' ; // For (A OR B) expressions

WS : [ \t\r\n]+ -> skip;

// --- PARAM_MODE (mode 1) ---
mode PARAM_MODE;

    // 1. Matches the closing ')' and pops the mode
    RPAREN_PARAM : ')' -> type(RPAREN), popMode ;

    // 2. This rule understands escape characters
    ParameterText : ( '\\' . | ~[\\)] )+ ;