parser grammar BooleanConditionParser;

options { tokenVocab = BooleanConditionLexer; }

// Parser Rules
evaluator : expression EOF;

expression : expression OR andExpression | andExpression;

andExpression : andExpression AND notExpression | notExpression;

notExpression : '!' notExpression | atom;

atom : LPAREN expression RPAREN // For grouping: (A OR B)
     | conditionCall
     ;

conditionCall
    // The 'pushMode' command must reference the Lexer name
    : Identifier
      { ((Lexer)_input.getTokenSource()).pushMode(BooleanConditionLexer.PARAM_MODE); }
      LPAREN
      parameter
      RPAREN
    | Identifier // The form without parentheses
    ;

parameter : ParameterText? ;