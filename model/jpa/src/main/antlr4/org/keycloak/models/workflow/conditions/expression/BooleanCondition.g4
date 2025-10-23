grammar BooleanCondition;

// Parser Rules
evaluator : expression EOF;

expression : expression OR andExpression | andExpression;
andExpression : andExpression AND notExpression | notExpression;
notExpression : '!' notExpression | atom;

atom : LPAREN expression RPAREN
     | conditionCall
     ;

conditionCall : Identifier LPAREN parameterList? RPAREN ;
parameterList : StringLiteral (COMMA StringLiteral)* ;

// Lexer Rules
OR : 'OR';
AND : 'AND';
NOT : '!';

Identifier : [a-zA-Z_][a-zA-Z_0-9-]*;
StringLiteral : '"' ( ~'"' | '""' )* '"' ;

// Explicitly defined tokens for the characters
LPAREN : '(';
RPAREN : ')';
COMMA : ',';

WS : [ \t\r\n]+ -> skip;