/**
 * @fileoverview Validate closing tag location in JSX
 * @author Ross Solomon
 */

'use strict';

const astUtil = require('../util/ast');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  onOwnLine: 'Closing tag of a multiline JSX expression must be on its own line.',
  matchIndent: 'Expected closing tag to match indentation of opening.',
};

module.exports = {
  meta: {
    docs: {
      description: 'Validate closing tag location for multiline JSX',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-closing-tag-location'),
    },
    fixable: 'whitespace',
    messages,
  },

  create(context) {
    function handleClosingElement(node) {
      if (!node.parent) {
        return;
      }

      const opening = node.parent.openingElement || node.parent.openingFragment;
      if (opening.loc.start.line === node.loc.start.line) {
        return;
      }

      if (opening.loc.start.column === node.loc.start.column) {
        return;
      }

      const messageId = astUtil.isNodeFirstInLine(context, node)
        ? 'matchIndent'
        : 'onOwnLine';
      report(context, messages[messageId], messageId, {
        node,
        loc: node.loc,
        fix(fixer) {
          const indent = Array(opening.loc.start.column + 1).join(' ');
          if (astUtil.isNodeFirstInLine(context, node)) {
            return fixer.replaceTextRange(
              [node.range[0] - node.loc.start.column, node.range[0]],
              indent
            );
          }

          return fixer.insertTextBefore(node, `\n${indent}`);
        },
      });
    }

    return {
      JSXClosingElement: handleClosingElement,
      JSXClosingFragment: handleClosingElement,
    };
  },
};
