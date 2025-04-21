/**
 * @fileoverview Forbid certain props on DOM Nodes
 * @author David Vázquez
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const DEFAULTS = [];

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  propIsForbidden: 'Prop "{{prop}}" is forbidden on DOM Nodes',
};

module.exports = {
  meta: {
    docs: {
      description: 'Forbid certain props on DOM Nodes',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('forbid-dom-props'),
    },

    messages,

    schema: [{
      type: 'object',
      properties: {
        forbid: {
          type: 'array',
          items: {
            oneOf: [{
              type: 'string',
            }, {
              type: 'object',
              properties: {
                propName: {
                  type: 'string',
                },
                message: {
                  type: 'string',
                },
              },
            }],
            minLength: 1,
          },
          uniqueItems: true,
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    const configuration = context.options[0] || {};
    const forbid = new Map((configuration.forbid || DEFAULTS).map((value) => {
      const propName = typeof value === 'string' ? value : value.propName;
      const options = {
        message: typeof value === 'string' ? null : value.message,
      };
      return [propName, options];
    }));

    function isForbidden(prop) {
      return forbid.has(prop);
    }

    return {
      JSXAttribute(node) {
        const tag = node.parent.name.name;
        if (!(tag && typeof tag === 'string' && tag[0] !== tag[0].toUpperCase())) {
          // This is a Component, not a DOM node, so exit.
          return;
        }

        const prop = node.name.name;

        if (!isForbidden(prop)) {
          return;
        }

        const customMessage = forbid.get(prop).message;

        report(context, customMessage || messages.propIsForbidden, !customMessage && 'propIsForbidden', {
          node,
          data: {
            prop,
          },
        });
      },
    };
  },
};
