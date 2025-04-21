/**
 * @fileoverview Validate props indentation in JSX
 * @author Yannick Croissant

 * This rule has been ported and modified from eslint and nodeca.
 * @author Vitaly Puzrin
 * @author Gyandeep Singh
 * @copyright 2015 Vitaly Puzrin. All rights reserved.
 * @copyright 2015 Gyandeep Singh. All rights reserved.
 Copyright (C) 2014 by Vitaly Puzrin

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the 'Software'), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

'use strict';

const astUtil = require('../util/ast');
const docsUrl = require('../util/docsUrl');
const reportC = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  wrongIndent: 'Expected indentation of {{needed}} {{type}} {{characters}} but found {{gotten}}.',
};

module.exports = {
  meta: {
    docs: {
      description: 'Validate props indentation in JSX',
      category: 'Stylistic Issues',
      recommended: false,
      url: docsUrl('jsx-indent-props'),
    },
    fixable: 'code',

    messages,

    schema: [{
      oneOf: [{
        enum: ['tab', 'first'],
      }, {
        type: 'integer',
      }, {
        type: 'object',
        properties: {
          indentMode: {
            oneOf: [{
              enum: ['tab', 'first'],
            }, {
              type: 'integer',
            }],
          },
          ignoreTernaryOperator: {
            type: 'boolean',
          },
        },
      }],
    }],
  },

  create(context) {
    const extraColumnStart = 0;
    let indentType = 'space';
    /** @type {number|'first'} */
    let indentSize = 4;
    const line = {
      isUsingOperator: false,
      currentOperator: false,
    };
    let ignoreTernaryOperator = false;

    if (context.options.length) {
      const isConfigObject = typeof context.options[0] === 'object';
      const indentMode = isConfigObject
        ? context.options[0].indentMode
        : context.options[0];

      if (indentMode === 'first') {
        indentSize = 'first';
        indentType = 'space';
      } else if (indentMode === 'tab') {
        indentSize = 1;
        indentType = 'tab';
      } else if (typeof indentMode === 'number') {
        indentSize = indentMode;
        indentType = 'space';
      }

      if (isConfigObject && context.options[0].ignoreTernaryOperator) {
        ignoreTernaryOperator = true;
      }
    }

    /**
     * Reports a given indent violation and properly pluralizes the message
     * @param {ASTNode} node Node violating the indent rule
     * @param {Number} needed Expected indentation character count
     * @param {Number} gotten Indentation character count in the actual node/code
     */
    function report(node, needed, gotten) {
      const msgContext = {
        needed,
        type: indentType,
        characters: needed === 1 ? 'character' : 'characters',
        gotten,
      };

      reportC(context, messages.wrongIndent, 'wrongIndent', {
        node,
        data: msgContext,
        fix(fixer) {
          return fixer.replaceTextRange([node.range[0] - node.loc.start.column, node.range[0]],
            Array(needed + 1).join(indentType === 'space' ? ' ' : '\t'));
        },
      });
    }

    /**
     * Get node indent
     * @param {ASTNode} node Node to examine
     * @return {Number} Indent
     */
    function getNodeIndent(node) {
      let src = context.getSourceCode().getText(node, node.loc.start.column + extraColumnStart);
      const lines = src.split('\n');
      src = lines[0];

      let regExp;
      if (indentType === 'space') {
        regExp = /^[ ]+/;
      } else {
        regExp = /^[\t]+/;
      }

      const indent = regExp.exec(src);
      const useOperator = /^([ ]|[\t])*[:]/.test(src) || /^([ ]|[\t])*[?]/.test(src);
      const useBracket = /[<]/.test(src);

      line.currentOperator = false;
      if (useOperator) {
        line.isUsingOperator = true;
        line.currentOperator = true;
      } else if (useBracket) {
        line.isUsingOperator = false;
      }

      return indent ? indent[0].length : 0;
    }

    /**
     * Check indent for nodes list
     * @param {ASTNode[]} nodes list of node objects
     * @param {Number} indent needed indent
     */
    function checkNodesIndent(nodes, indent) {
      nodes.forEach((node) => {
        const nodeIndent = getNodeIndent(node);
        if (line.isUsingOperator && !line.currentOperator && indentSize !== 'first' && !ignoreTernaryOperator) {
          indent += indentSize;
          line.isUsingOperator = false;
        }
        if (
          node.type !== 'ArrayExpression' && node.type !== 'ObjectExpression'
          && nodeIndent !== indent && astUtil.isNodeFirstInLine(context, node)
        ) {
          report(node, indent, nodeIndent);
        }
      });
    }

    return {
      JSXOpeningElement(node) {
        if (!node.attributes.length) {
          return;
        }
        let propIndent;
        if (indentSize === 'first') {
          const firstPropNode = node.attributes[0];
          propIndent = firstPropNode.loc.start.column;
        } else {
          const elementIndent = getNodeIndent(node);
          propIndent = elementIndent + indentSize;
        }
        checkNodesIndent(node.attributes, propIndent);
      },
    };
  },
};
