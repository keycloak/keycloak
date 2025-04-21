/**
 * @fileoverview Require or prevent a new line after jsx elements and expressions.
 * @author Johnny Zabala
 * @author Joseph Stiles
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  require: 'JSX element should start in a new line',
  prevent: 'JSX element should not start in a new line',
};

module.exports = {
  meta: {
    docs: {
      description: 'Require or prevent a new line after jsx elements and expressions.',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-newline'),
    },
    fixable: 'code',

    messages,
    schema: [
      {
        type: 'object',
        properties: {
          prevent: {
            default: false,
            type: 'boolean',
          },
        },
        additionalProperties: false,
      },
    ],
  },
  create(context) {
    const jsxElementParents = new Set();
    const sourceCode = context.getSourceCode();
    return {
      'Program:exit'() {
        jsxElementParents.forEach((parent) => {
          parent.children.forEach((element, index, elements) => {
            if (element.type === 'JSXElement' || element.type === 'JSXExpressionContainer') {
              const firstAdjacentSibling = elements[index + 1];
              const secondAdjacentSibling = elements[index + 2];

              const hasSibling = firstAdjacentSibling
              && secondAdjacentSibling
              && (firstAdjacentSibling.type === 'Literal' || firstAdjacentSibling.type === 'JSXText');

              if (!hasSibling) return;

              // Check adjacent sibling has the proper amount of newlines
              const isWithoutNewLine = !/\n\s*\n/.test(firstAdjacentSibling.value);

              const prevent = !!(context.options[0] || {}).prevent;

              if (isWithoutNewLine === prevent) return;

              const messageId = prevent
                ? 'prevent'
                : 'require';

              const regex = prevent
                ? /(\n\n)(?!.*\1)/g
                : /(\n)(?!.*\1)/g;

              const replacement = prevent
                ? '\n'
                : '\n\n';

              report(context, messages[messageId], messageId, {
                node: secondAdjacentSibling,
                fix(fixer) {
                  return fixer.replaceText(
                    firstAdjacentSibling,
                    // double or remove the last newline
                    sourceCode.getText(firstAdjacentSibling)
                      .replace(regex, replacement)
                  );
                },
              });
            }
          });
        });
      },
      ':matches(JSXElement, JSXFragment) > :matches(JSXElement, JSXExpressionContainer)': (node) => {
        jsxElementParents.add(node.parent);
      },
    };
  },
};
