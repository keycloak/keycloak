parser grammar ScimFilterParser;

options { tokenVocab = ScimFilterLexer; }

// Entry point
filter : expression EOF;

// Logical expressions (precedence: OR < AND < NOT)
expression
    : expression OR andExpression
    | andExpression
    ;

andExpression
    : andExpression AND notExpression
    | notExpression
    ;

notExpression
    : NOT notExpression
    | atom
    ;

atom
    : LPAREN expression RPAREN
    | attributeExpression
    ;

// Attribute comparison expressions
attributeExpression
    : ATTRPATH PR                              # PresentExpression
    | ATTRPATH compareOp compValue             # ComparisonExpression
    ;

compareOp
    : EQ | NE | CO | SW | EW | GT | GE | LT | LE
    ;

compValue
    : FALSE | NULL | TRUE | NUMBER | STRING
    ;
