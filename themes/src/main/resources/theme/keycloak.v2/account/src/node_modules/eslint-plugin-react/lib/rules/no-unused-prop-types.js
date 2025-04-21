/**
 * @fileoverview Prevent definitions of unused prop types
 * @author Evgueni Naverniouk
 */

'use strict';

// As for exceptions for props.children or props.className (and alike) look at
// https://github.com/jsx-eslint/eslint-plugin-react/issues/7

const Components = require('../util/Components');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  unusedPropType: '\'{{name}}\' PropType is defined but prop is never used',
};

module.exports = {
  meta: {
    docs: {
      description: 'Prevent definitions of unused prop types',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('no-unused-prop-types'),
    },

    messages,

    schema: [{
      type: 'object',
      properties: {
        ignore: {
          type: 'array',
          items: {
            type: 'string',
          },
          uniqueItems: true,
        },
        customValidators: {
          type: 'array',
          items: {
            type: 'string',
          },
        },
        skipShapeProps: {
          type: 'boolean',
        },
      },
      additionalProperties: false,
    }],
  },

  create: Components.detect((context, components) => {
    const defaults = { skipShapeProps: true, customValidators: [], ignore: [] };
    const configuration = Object.assign({}, defaults, context.options[0] || {});

    /**
     * Checks if the prop is ignored
     * @param {String} name Name of the prop to check.
     * @returns {Boolean} True if the prop is ignored, false if not.
     */
    function isIgnored(name) {
      return configuration.ignore.indexOf(name) !== -1;
    }

    /**
     * Checks if the component must be validated
     * @param {Object} component The component to process
     * @returns {Boolean} True if the component must be validated, false if not.
     */
    function mustBeValidated(component) {
      return Boolean(
        component
        && !component.ignoreUnusedPropTypesValidation
      );
    }

    /**
     * Checks if a prop is used
     * @param {ASTNode} node The AST node being checked.
     * @param {Object} prop Declared prop object
     * @returns {Boolean} True if the prop is used, false if not.
     */
    function isPropUsed(node, prop) {
      const usedPropTypes = node.usedPropTypes || [];
      for (let i = 0, l = usedPropTypes.length; i < l; i++) {
        const usedProp = usedPropTypes[i];
        if (
          prop.type === 'shape'
          || prop.type === 'exact'
          || prop.name === '__ANY_KEY__'
          || usedProp.name === prop.name
        ) {
          return true;
        }
      }

      return false;
    }

    /**
     * Used to recursively loop through each declared prop type
     * @param {Object} component The component to process
     * @param {ASTNode[]|true} props List of props to validate
     */
    function reportUnusedPropType(component, props) {
      // Skip props that check instances
      if (props === true) {
        return;
      }

      Object.keys(props || {}).forEach((key) => {
        const prop = props[key];
        // Skip props that check instances
        if (prop === true) {
          return;
        }

        if ((prop.type === 'shape' || prop.type === 'exact') && configuration.skipShapeProps) {
          return;
        }

        if (prop.node && prop.node.typeAnnotation && prop.node.typeAnnotation.typeAnnotation
          && prop.node.typeAnnotation.typeAnnotation.type === 'TSNeverKeyword') {
          return;
        }

        if (prop.node && !isIgnored(prop.fullName) && !isPropUsed(component, prop)) {
          report(context, messages.unusedPropType, 'unusedPropType', {
            node: prop.node.key || prop.node,
            data: {
              name: prop.fullName,
            },
          });
        }

        if (prop.children) {
          reportUnusedPropType(component, prop.children);
        }
      });
    }

    /**
     * Reports unused proptypes for a given component
     * @param {Object} component The component to process
     */
    function reportUnusedPropTypes(component) {
      reportUnusedPropType(component, component.declaredPropTypes);
    }

    // --------------------------------------------------------------------------
    // Public
    // --------------------------------------------------------------------------

    return {
      'Program:exit'() {
        const list = components.list();
        // Report undeclared proptypes for all classes
        Object.keys(list).filter((component) => mustBeValidated(list[component])).forEach((component) => {
          if (!mustBeValidated(list[component])) {
            return;
          }
          reportUnusedPropTypes(list[component]);
        });
      },
    };
  }),
};
