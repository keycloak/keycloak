/**
 * @fileoverview Prevent usage of isMounted
 * @author Joe Lencioni
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  noIsMounted: 'Do not use isMounted',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent usage of isMounted',
      category: 'Best Practices',
      recommended: true,
      url: docsUrl('no-is-mounted'),
    },

    messages,

    schema: [],
  },

  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        if (callee.type !== 'MemberExpression') {
          return;
        }
        if (callee.object.type !== 'ThisExpression' || callee.property.name !== 'isMounted') {
          return;
        }
        const ancestors = context.getAncestors(callee);
        for (let i = 0, j = ancestors.length; i < j; i++) {
          if (ancestors[i].type === 'Property' || ancestors[i].type === 'MethodDefinition') {
            report(context, messages.noIsMounted, 'noIsMounted', {
              node: callee,
            });
            break;
          }
        }
      },
    };
  },
};
