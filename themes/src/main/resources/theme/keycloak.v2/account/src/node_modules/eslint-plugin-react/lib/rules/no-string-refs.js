/**
 * @fileoverview Prevent string definitions for references and prevent referencing this.refs
 * @author Tom Hastjarjanto
 */

'use strict';

const componentUtil = require('../util/componentUtil');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  thisRefsDeprecated: 'Using this.refs is deprecated.',
  stringInRefDeprecated: 'Using string literals in ref attributes is deprecated.',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent string definitions for references and prevent referencing this.refs',
      category: 'Best Practices',
      recommended: true,
      url: docsUrl('no-string-refs'),
    },

    messages,

    schema: [{
      type: 'object',
      properties: {
        noTemplateLiterals: {
          type: 'boolean',
        },
      },
      additionalProperties: false,
    }],
  },

  create(context) {
    const detectTemplateLiterals = context.options[0] ? context.options[0].noTemplateLiterals : false;
    /**
     * Checks if we are using refs
     * @param {ASTNode} node The AST node being checked.
     * @returns {Boolean} True if we are using refs, false if not.
     */
    function isRefsUsage(node) {
      return !!(
        (componentUtil.getParentES6Component(context) || componentUtil.getParentES5Component(context))
        && node.object.type === 'ThisExpression'
        && node.property.name === 'refs'
      );
    }

    /**
     * Checks if we are using a ref attribute
     * @param {ASTNode} node The AST node being checked.
     * @returns {Boolean} True if we are using a ref attribute, false if not.
     */
    function isRefAttribute(node) {
      return !!(
        node.type === 'JSXAttribute'
        && node.name
        && node.name.name === 'ref'
      );
    }

    /**
     * Checks if a node contains a string value
     * @param {ASTNode} node The AST node being checked.
     * @returns {Boolean} True if the node contains a string value, false if not.
     */
    function containsStringLiteral(node) {
      return !!(
        node.value
        && node.value.type === 'Literal'
        && typeof node.value.value === 'string'
      );
    }

    /**
     * Checks if a node contains a string value within a jsx expression
     * @param {ASTNode} node The AST node being checked.
     * @returns {Boolean} True if the node contains a string value within a jsx expression, false if not.
     */
    function containsStringExpressionContainer(node) {
      return !!(
        node.value
        && node.value.type === 'JSXExpressionContainer'
        && node.value.expression
        && ((node.value.expression.type === 'Literal' && typeof node.value.expression.value === 'string')
        || (node.value.expression.type === 'TemplateLiteral' && detectTemplateLiterals))
      );
    }

    return {
      MemberExpression(node) {
        if (isRefsUsage(node)) {
          report(context, messages.thisRefsDeprecated, 'thisRefsDeprecated', {
            node,
          });
        }
      },
      JSXAttribute(node) {
        if (
          isRefAttribute(node)
          && (containsStringLiteral(node) || containsStringExpressionContainer(node))
        ) {
          report(context, messages.stringInRefDeprecated, 'stringInRefDeprecated', {
            node,
          });
        }
      },
    };
  },
};
