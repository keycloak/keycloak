/**
 * @fileoverview Ensure proper position of the first property in JSX
 * @author Joachim Seminck
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  propOnNewLine: 'Property should be placed on a new line',
  propOnSameLine: 'Property should be placed on the same line as the component declaration',
};

module.exports = {
  meta: {
    docs: {
      description: 'Ensure proper position of the first property in JSX',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-first-prop-new-line'),
    },
    fixable: 'code',

    messages,

    schema: [{
      enum: ['always', 'never', 'multiline', 'multiline-multiprop'],
    }],
  },

  create(context) {
    const configuration = context.options[0] || 'multiline-multiprop';

    function isMultilineJSX(jsxNode) {
      return jsxNode.loc.start.line < jsxNode.loc.end.line;
    }

    return {
      JSXOpeningElement(node) {
        if (
          (configuration === 'multiline' && isMultilineJSX(node))
          || (configuration === 'multiline-multiprop' && isMultilineJSX(node) && node.attributes.length > 1)
          || (configuration === 'always')
        ) {
          node.attributes.some((decl) => {
            if (decl.loc.start.line === node.loc.start.line) {
              report(context, messages.propOnNewLine, 'propOnNewLine', {
                node: decl,
                fix(fixer) {
                  return fixer.replaceTextRange([node.name.range[1], decl.range[0]], '\n');
                },
              });
            }
            return true;
          });
        } else if (configuration === 'never' && node.attributes.length > 0) {
          const firstNode = node.attributes[0];
          if (node.loc.start.line < firstNode.loc.start.line) {
            report(context, messages.propOnSameLine, 'propOnSameLine', {
              node: firstNode,
              fix(fixer) {
                return fixer.replaceTextRange([node.name.range[1], firstNode.range[0]], ' ');
              },
            });
          }
        }
      },
    };
  },
};
