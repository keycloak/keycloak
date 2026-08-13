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
    | valuePath
    | attributeExpression
    ;

// Value path for complex attribute filtering (RFC 7644 §3.4.2.2)
// e.g., name[familyName eq "Smith" and givenName sw "Jo"]
valuePath
    : ATTRPATH LBRACKET expression RBRACKET
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
