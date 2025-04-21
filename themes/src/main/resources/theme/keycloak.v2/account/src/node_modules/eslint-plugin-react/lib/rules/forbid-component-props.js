/**
 * @fileoverview Forbid certain props on components
 * @author Joe Lencioni
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const DEFAULTS = ['className', 'style'];

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  propIsForbidden: 'Prop "{{prop}}" is forbidden on Components',
};

module.exports = {
  meta: {
    docs: {
      description: 'Forbid certain props on components',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('forbid-component-props'),
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
                allowedFor: {
                  type: 'array',
                  uniqueItems: true,
                  items: {
                    type: 'string',
                  },
                },
                message: {
                  type: 'string',
                },
              },
            }],
          },
        },
      },
    }],
  },

  create(context) {
    const configuration = context.options[0] || {};
    const forbid = new Map((configuration.forbid || DEFAULTS).map((value) => {
      const propName = typeof value === 'string' ? value : value.propName;
      const options = {
        allowList: typeof value === 'string' ? [] : (value.allowedFor || []),
        message: typeof value === 'string' ? null : value.message,
      };
      return [propName, options];
    }));

    function isForbidden(prop, tagName) {
      const options = forbid.get(prop);
      const allowList = options ? options.allowList : undefined;
      // if the tagName is undefined (`<this.something>`), we assume it's a forbidden element
      return typeof allowList !== 'undefined' && (typeof tagName === 'undefined' || allowList.indexOf(tagName) === -1);
    }

    return {
      JSXAttribute(node) {
        const parentName = node.parent.name;
        // Extract a component name when using a "namespace", e.g. `<AntdLayout.Content />`.
        const tag = parentName.name || `${parentName.object.name}.${parentName.property.name}`;
        const componentName = parentName.name || parentName.property.name;
        if (componentName && typeof componentName[0] === 'string' && componentName[0] !== componentName[0].toUpperCase()) {
          // This is a DOM node, not a Component, so exit.
          return;
        }

        const prop = node.name.name;

        if (!isForbidden(prop, tag)) {
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
