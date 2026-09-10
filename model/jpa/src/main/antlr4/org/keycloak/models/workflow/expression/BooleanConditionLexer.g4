lexer grammar BooleanConditionLexer;

// --- DEFAULT_MODE (mode 0) ---

// CASE-INSENSITIVE OPERATORS
// We use character sets like [oO] to match both cases.

OR  : [oO] [rR];             // Matches: or, OR, Or, oR
AND : [aA] [nN] [dD];        // Matches: and, AND, And, ...
NOT : [nN] [oO] [tT];        // Matches: not, NOT, Not, ...

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