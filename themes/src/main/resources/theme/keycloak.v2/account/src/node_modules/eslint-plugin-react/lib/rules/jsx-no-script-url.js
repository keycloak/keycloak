/**
 * @fileoverview Prevent usage of `javascript:` URLs
 * @author Sergei Startsev
 */

'use strict';

const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

// https://github.com/facebook/react/blob/d0ebde77f6d1232cefc0da184d731943d78e86f2/packages/react-dom/src/shared/sanitizeURL.js#L30
/* eslint-disable-next-line max-len, no-control-regex */
const isJavaScriptProtocol = /^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;

function hasJavaScriptProtocol(attr) {
  return attr.value && attr.value.type === 'Literal'
    && isJavaScriptProtocol.test(attr.value.value);
}

function shouldVerifyElement(node, config) {
  const name = node.name && node.name.name;
  return name === 'a' || config.find((i) => i.name === name);
}

function shouldVerifyProp(node, config) {
  const name = node.name && node.name.name;
  const parentName = node.parent.name && node.parent.name.name;

  if (parentName === 'a' && name === 'href') {
    return true;
  }

  const el = config.find((i) => i.name === parentName);
  if (!el) {
    return false;
  }

  const props = el.props || [];
  return node.name && props.indexOf(name) !== -1;
}

const messages = {
  noScriptURL: 'A future version of React will block javascript: URLs as a security precaution. Use event handlers instead if you can. If you need to generate unsafe HTML, try using dangerouslySetInnerHTML instead.',
};

module.exports = {
  meta: {
    docs: {
      description: 'Forbid `javascript:` URLs',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('jsx-no-script-url'),
    },

    messages,

    schema: [{
      type: 'array',
      uniqueItems: true,
      items: {
        type: 'object',
        properties: {
          name: {
            type: 'string',
          },
          props: {
            type: 'array',
            items: {
              type: 'string',
              uniqueItems: true,
            },
          },
        },
        required: ['name', 'props'],
        additionalProperties: false,
      },
    }],
  },

  create(context) {
    const config = context.options[0] || [];
    return {
      JSXAttribute(node) {
        const parent = node.parent;
        if (shouldVerifyElement(parent, config) && shouldVerifyProp(node, config) && hasJavaScriptProtocol(node)) {
          report(context, messages.noScriptURL, 'noScriptURL', {
            node,
          });
        }
      },
    };
  },
};
