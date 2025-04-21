/**
 * @fileoverview Prevent missing displayName in a React component definition
 * @author Yannick Croissant
 */

'use strict';

const values = require('object.values');

const Components = require('../util/Components');
const astUtil = require('../util/ast');
const componentUtil = require('../util/componentUtil');
const docsUrl = require('../util/docsUrl');
const propsUtil = require('../util/props');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  noDisplayName: 'Component definition is missing display name',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent missing displayName in a React component definition',
      category: 'Best Practices',
      recommended: true,
      url: docsUrl('display-name'),
    },

    messages,

    schema: [{
      type: 'object',
      properties: {
        ignoreTranspilerName: {
          type: 'boolean',
        },
      },
      additionalProperties: false,
    }],
  },

  create: Components.detect((context, components, utils) => {
    const config = context.options[0] || {};
    const ignoreTranspilerName = config.ignoreTranspilerName || false;

    /**
     * Mark a prop type as declared
     * @param {ASTNode} node The AST node being checked.
     */
    function markDisplayNameAsDeclared(node) {
      components.set(node, {
        hasDisplayName: true,
      });
    }

    /**
     * Reports missing display name for a given component
     * @param {Object} component The component to process
     */
    function reportMissingDisplayName(component) {
      report(context, messages.noDisplayName, 'noDisplayName', {
        node: component.node,
      });
    }

    /**
     * Checks if the component have a name set by the transpiler
     * @param {ASTNode} node The AST node being checked.
     * @returns {Boolean} True if component has a name, false if not.
     */
    function hasTranspilerName(node) {
      const namedObjectAssignment = (
        node.type === 'ObjectExpression'
        && node.parent
        && node.parent.parent
        && node.parent.parent.type === 'AssignmentExpression'
        && (
          !node.parent.parent.left.object
          || node.parent.parent.left.object.name !== 'module'
          || node.parent.parent.left.property.name !== 'exports'
        )
      );
      const namedObjectDeclaration = (
        node.type === 'ObjectExpression'
        && node.parent
        && node.parent.parent
        && node.parent.parent.type === 'VariableDeclarator'
      );
      const namedClass = (
        (node.type === 'ClassDeclaration' || node.type === 'ClassExpression')
        && node.id
        && !!node.id.name
      );

      const namedFunctionDeclaration = (
        (node.type === 'FunctionDeclaration' || node.type === 'FunctionExpression')
        && node.id
        && !!node.id.name
      );

      const namedFunctionExpression = (
        astUtil.isFunctionLikeExpression(node)
        && node.parent
        && (node.parent.type === 'VariableDeclarator' || node.parent.type === 'Property' || node.parent.method === true)
        && (!node.parent.parent || !componentUtil.isES5Component(node.parent.parent, context))
      );

      if (
        namedObjectAssignment || namedObjectDeclaration
        || namedClass
        || namedFunctionDeclaration || namedFunctionExpression
      ) {
        return true;
      }
      return false;
    }

    // --------------------------------------------------------------------------
    // Public
    // --------------------------------------------------------------------------

    return {
      'ClassProperty, PropertyDefinition'(node) {
        if (!propsUtil.isDisplayNameDeclaration(node)) {
          return;
        }
        markDisplayNameAsDeclared(node);
      },

      MemberExpression(node) {
        if (!propsUtil.isDisplayNameDeclaration(node.property)) {
          return;
        }
        const component = utils.getRelatedComponent(node);
        if (!component) {
          return;
        }
        markDisplayNameAsDeclared(component.node.type === 'TSAsExpression' ? component.node.expression : component.node);
      },

      FunctionExpression(node) {
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          return;
        }
        if (components.get(node)) {
          markDisplayNameAsDeclared(node);
        }
      },

      FunctionDeclaration(node) {
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          return;
        }
        if (components.get(node)) {
          markDisplayNameAsDeclared(node);
        }
      },

      ArrowFunctionExpression(node) {
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          return;
        }

        if (components.get(node)) {
          markDisplayNameAsDeclared(node);
        }
      },

      MethodDefinition(node) {
        if (!propsUtil.isDisplayNameDeclaration(node.key)) {
          return;
        }
        markDisplayNameAsDeclared(node);
      },

      ClassExpression(node) {
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          return;
        }
        markDisplayNameAsDeclared(node);
      },

      ClassDeclaration(node) {
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          return;
        }
        markDisplayNameAsDeclared(node);
      },

      ObjectExpression(node) {
        if (!componentUtil.isES5Component(node, context)) {
          return;
        }
        if (ignoreTranspilerName || !hasTranspilerName(node)) {
          // Search for the displayName declaration
          node.properties.forEach((property) => {
            if (!property.key || !propsUtil.isDisplayNameDeclaration(property.key)) {
              return;
            }
            markDisplayNameAsDeclared(node);
          });
          return;
        }
        markDisplayNameAsDeclared(node);
      },

      CallExpression(node) {
        if (!utils.isPragmaComponentWrapper(node)) {
          return;
        }

        if (node.arguments.length > 0 && astUtil.isFunctionLikeExpression(node.arguments[0])) {
          // Skip over React.forwardRef declarations that are embeded within
          // a React.memo i.e. React.memo(React.forwardRef(/* ... */))
          // This means that we raise a single error for the call to React.memo
          // instead of one for React.memo and one for React.forwardRef
          const isWrappedInAnotherPragma = utils.getPragmaComponentWrapper(node);
          if (
            !isWrappedInAnotherPragma
            && (ignoreTranspilerName || !hasTranspilerName(node.arguments[0]))
          ) {
            return;
          }

          if (components.get(node)) {
            markDisplayNameAsDeclared(node);
          }
        }
      },

      'Program:exit'() {
        const list = components.list();
        // Report missing display name for all components
        values(list).filter((component) => !component.hasDisplayName).forEach((component) => {
          reportMissingDisplayName(component);
        });
      },
    };
  }),
};
