/**
 * @fileoverview Forbid certain elements
 * @author Kenneth Chung
 */

'use strict';

const has = require('object.hasown/polyfill')();
const docsUrl = require('../util/docsUrl');
const isCreateElement = require('../util/isCreateElement');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  forbiddenElement: '<{{element}}> is forbidden',
  forbiddenElement_message: '<{{element}}> is forbidden, {{message}}',
};

module.exports = {
  meta: {
    docs: {
      description: 'Forbid certain elements',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('forbid-elements'),
    },

    messages,

    schema: [{
      type: 'object',
      properties: {
        forbid: {
          type: 'array',
          items: {
            anyOf: [
              { type: 'string' },
              {
                type: 'object',
                properties: {
                  element: { type: 'string' },
                  message: { type: 'string' },
                },
                required: ['element'],
                additionalProperties: false,
              },
            ],
          },
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    const configuration = context.options[0] || {};
    const forbidConfiguration = configuration.forbid || [];

    const indexedForbidConfigs = {};

    forbidConfiguration.forEach((item) => {
      if (typeof item === 'string') {
        indexedForbidConfigs[item] = { element: item };
      } else {
        indexedForbidConfigs[item.element] = item;
      }
    });

    function reportIfForbidden(element, node) {
      if (has(indexedForbidConfigs, element)) {
        const message = indexedForbidConfigs[element].message;

        report(
          context,
          message ? messages.forbiddenElement_message : messages.forbiddenElement,
          message ? 'forbiddenElement_message' : 'forbiddenElement',
          {
            node,
            data: {
              element,
              message,
            },
          }
        );
      }
    }

    return {
      JSXOpeningElement(node) {
        reportIfForbidden(context.getSourceCode().getText(node.name), node.name);
      },

      CallExpression(node) {
        if (!isCreateElement(node, context)) {
          return;
        }

        const argument = node.arguments[0];
        const argType = argument.type;

        if (argType === 'Identifier' && /^[A-Z_]/.test(argument.name)) {
          reportIfForbidden(argument.name, argument);
        } else if (argType === 'Literal' && /^[a-z][^.]*$/.test(argument.value)) {
          reportIfForbidden(argument.value, argument);
        } else if (argType === 'MemberExpression') {
          reportIfForbidden(context.getSourceCode().getText(argument), argument);
        }
      },
    };
  },
};
