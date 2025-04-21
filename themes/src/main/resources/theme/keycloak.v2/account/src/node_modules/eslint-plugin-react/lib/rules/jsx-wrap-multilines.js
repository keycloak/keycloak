/**
 * @fileoverview Prevent missing parentheses around multilines JSX
 * @author Yannick Croissant
 */

'use strict';

const has = require('object.hasown/polyfill')();
const docsUrl = require('../util/docsUrl');
const jsxUtil = require('../util/jsx');
const reportC = require('../util/report');
const isParenthesized = require('../util/ast').isParenthesized;

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const DEFAULTS = {
  declaration: 'parens',
  assignment: 'parens',
  return: 'parens',
  arrow: 'parens',
  condition: 'ignore',
  logical: 'ignore',
  prop: 'ignore',
};

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  missingParens: 'Missing parentheses around multilines JSX',
  parensOnNewLines: 'Parentheses around JSX should be on separate lines',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent missing parentheses around multilines JSX',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-wrap-multilines'),
    },
    fixable: 'code',

    messages,

    schema: [{
      type: 'object',
      // true/false are for backwards compatibility
      properties: {
        declaration: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        assignment: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        return: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        arrow: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        condition: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        logical: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
        prop: {
          enum: [true, false, 'ignore', 'parens', 'parens-new-line'],
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    function getOption(type) {
      const userOptions = context.options[0] || {};
      if (has(userOptions, type)) {
        return userOptions[type];
      }
      return DEFAULTS[type];
    }

    function isEnabled(type) {
      const option = getOption(type);
      return option && option !== 'ignore';
    }

    function needsOpeningNewLine(node) {
      const previousToken = context.getSourceCode().getTokenBefore(node);

      if (!isParenthesized(context, node)) {
        return false;
      }

      if (previousToken.loc.end.line === node.loc.start.line) {
        return true;
      }

      return false;
    }

    function needsClosingNewLine(node) {
      const nextToken = context.getSourceCode().getTokenAfter(node);

      if (!isParenthesized(context, node)) {
        return false;
      }

      if (node.loc.end.line === nextToken.loc.end.line) {
        return true;
      }

      return false;
    }

    function isMultilines(node) {
      return node.loc.start.line !== node.loc.end.line;
    }

    function report(node, messageId, fix) {
      reportC(context, messages[messageId], messageId, {
        node,
        fix,
      });
    }

    function trimTokenBeforeNewline(node, tokenBefore) {
      // if the token before the jsx is a bracket or curly brace
      // we don't want a space between the opening parentheses and the multiline jsx
      const isBracket = tokenBefore.value === '{' || tokenBefore.value === '[';
      return `${tokenBefore.value.trim()}${isBracket ? '' : ' '}`;
    }

    function check(node, type) {
      if (!node || !jsxUtil.isJSX(node)) {
        return;
      }

      const sourceCode = context.getSourceCode();
      const option = getOption(type);

      if ((option === true || option === 'parens') && !isParenthesized(context, node) && isMultilines(node)) {
        report(node, 'missingParens', (fixer) => fixer.replaceText(node, `(${sourceCode.getText(node)})`));
      }

      if (option === 'parens-new-line' && isMultilines(node)) {
        if (!isParenthesized(context, node)) {
          const tokenBefore = sourceCode.getTokenBefore(node, { includeComments: true });
          const tokenAfter = sourceCode.getTokenAfter(node, { includeComments: true });
          const start = node.loc.start;
          if (tokenBefore.loc.end.line < start.line) {
            // Strip newline after operator if parens newline is specified
            report(
              node,
              'missingParens',
              (fixer) => fixer.replaceTextRange(
                [tokenBefore.range[0], tokenAfter && (tokenAfter.value === ';' || tokenAfter.value === '}') ? tokenAfter.range[0] : node.range[1]],
                `${trimTokenBeforeNewline(node, tokenBefore)}(\n${start.column > 0 ? ' '.repeat(start.column) : ''}${sourceCode.getText(node)}\n${start.column > 0 ? ' '.repeat(start.column - 2) : ''})`
              )
            );
          } else {
            report(node, 'missingParens', (fixer) => fixer.replaceText(node, `(\n${sourceCode.getText(node)}\n)`));
          }
        } else {
          const needsOpening = needsOpeningNewLine(node);
          const needsClosing = needsClosingNewLine(node);
          if (needsOpening || needsClosing) {
            report(node, 'parensOnNewLines', (fixer) => {
              const text = sourceCode.getText(node);
              let fixed = text;
              if (needsOpening) {
                fixed = `\n${fixed}`;
              }
              if (needsClosing) {
                fixed = `${fixed}\n`;
              }
              return fixer.replaceText(node, fixed);
            });
          }
        }
      }
    }

    // --------------------------------------------------------------------------
    // Public
    // --------------------------------------------------------------------------

    return {

      VariableDeclarator(node) {
        const type = 'declaration';
        if (!isEnabled(type)) {
          return;
        }
        if (!isEnabled('condition') && node.init && node.init.type === 'ConditionalExpression') {
          check(node.init.consequent, type);
          check(node.init.alternate, type);
          return;
        }
        check(node.init, type);
      },

      AssignmentExpression(node) {
        const type = 'assignment';
        if (!isEnabled(type)) {
          return;
        }
        if (!isEnabled('condition') && node.right.type === 'ConditionalExpression') {
          check(node.right.consequent, type);
          check(node.right.alternate, type);
          return;
        }
        check(node.right, type);
      },

      ReturnStatement(node) {
        const type = 'return';
        if (isEnabled(type)) {
          check(node.argument, type);
        }
      },

      'ArrowFunctionExpression:exit': (node) => {
        const arrowBody = node.body;
        const type = 'arrow';

        if (isEnabled(type) && arrowBody.type !== 'BlockStatement') {
          check(arrowBody, type);
        }
      },

      ConditionalExpression(node) {
        const type = 'condition';
        if (isEnabled(type)) {
          check(node.consequent, type);
          check(node.alternate, type);
        }
      },

      LogicalExpression(node) {
        const type = 'logical';
        if (isEnabled(type)) {
          check(node.right, type);
        }
      },

      JSXAttribute(node) {
        const type = 'prop';
        if (isEnabled(type) && node.value && node.value.type === 'JSXExpressionContainer') {
          check(node.value.expression, type);
        }
      },
    };
  },
};
