/**
 * @fileoverview Prevent usage of findDOMNode
 * @author Yannick Croissant
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  noFindDOMNode: 'Do not use findDOMNode. It doesn’t work with function components and is deprecated in StrictMode. See https://reactjs.org/docs/react-dom.html#finddomnode',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent usage of findDOMNode',
      category: 'Best Practices',
      recommended: true,
      url: docsUrl('no-find-dom-node'),
    },

    messages,

    schema: [],
  },

  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;

        const isfindDOMNode = (callee.name === 'findDOMNode')
          || (callee.property && callee.property.name === 'findDOMNode');
        if (!isfindDOMNode) {
          return;
        }

        report(context, messages.noFindDOMNode, 'noFindDOMNode', {
          node: callee,
        });
      },
    };
  },
};
