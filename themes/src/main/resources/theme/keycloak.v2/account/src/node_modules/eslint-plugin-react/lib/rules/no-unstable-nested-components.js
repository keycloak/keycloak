/**
 * @fileoverview Prevent creating unstable components inside components
 * @author Ari Perkkiö
 */

'use strict';

const Components = require('../util/Components');
const docsUrl = require('../util/docsUrl');
const isCreateElement = require('../util/isCreateElement');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const COMPONENT_AS_PROPS_INFO = ' If you want to allow component creation in props, set allowAsProps option to true.';
const HOOK_REGEXP = /^use[A-Z0-9].*$/;

// ------------------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------------------

/**
 * Generate error message with given parent component name
 * @param {String} parentName Name of the parent component, if known
 * @returns {String} Error message with parent component name
 */
function generateErrorMessageWithParentName(parentName) {
  return `Do not define components during render. React will see a new component type on every render and destroy the entire subtree’s DOM nodes and state (https://reactjs.org/docs/reconciliation.html#elements-of-different-types). Instead, move this component definition out of the parent component${parentName ? ` “${parentName}” ` : ' '}and pass data as props.`;
}

/**
 * Check whether given text starts with `render`. Comparison is case-sensitive.
 * @param {String} text Text to validate
 * @returns {Boolean}
 */
function startsWithRender(text) {
  return (text || '').startsWith('render');
}

/**
 * Get closest parent matching given matcher
 * @param {ASTNode} node The AST node
 * @param {Context} context eslint context
 * @param {Function} matcher Method used to match the parent
 * @returns {ASTNode} The matching parent node, if any
 */
function getClosestMatchingParent(node, context, matcher) {
  if (!node || !node.parent || node.parent.type === 'Program') {
    return;
  }

  if (matcher(node.parent, context)) {
    return node.parent;
  }

  return getClosestMatchingParent(node.parent, context, matcher);
}

/**
 * Matcher used to check whether given node is a `createElement` call
 * @param {ASTNode} node The AST node
 * @param {Context} context eslint context
 * @returns {Boolean} True if node is a `createElement` call, false if not
 */
function isCreateElementMatcher(node, context) {
  return (
    node
    && node.type === 'CallExpression'
    && isCreateElement(node, context)
  );
}

/**
 * Matcher used to check whether given node is a `ObjectExpression`
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is a `ObjectExpression`, false if not
 */
function isObjectExpressionMatcher(node) {
  return node && node.type === 'ObjectExpression';
}

/**
 * Matcher used to check whether given node is a `JSXExpressionContainer`
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is a `JSXExpressionContainer`, false if not
 */
function isJSXExpressionContainerMatcher(node) {
  return node && node.type === 'JSXExpressionContainer';
}

/**
 * Matcher used to check whether given node is a `JSXAttribute` of `JSXExpressionContainer`
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is a `JSXAttribute` of `JSXExpressionContainer`, false if not
 */
function isJSXAttributeOfExpressionContainerMatcher(node) {
  return (
    node
    && node.type === 'JSXAttribute'
    && node.value
    && node.value.type === 'JSXExpressionContainer'
  );
}

/**
 * Matcher used to check whether given node is an object `Property`
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is a `Property`, false if not
 */
function isPropertyOfObjectExpressionMatcher(node) {
  return (
    node
    && node.parent
    && node.parent.type === 'Property'
  );
}

/**
 * Matcher used to check whether given node is a `CallExpression`
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is a `CallExpression`, false if not
 */
function isCallExpressionMatcher(node) {
  return node && node.type === 'CallExpression';
}

/**
 * Check whether given node or its parent is directly inside `map` call
 * ```jsx
 * {items.map(item => <li />)}
 * ```
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if node is directly inside `map` call, false if not
 */
function isMapCall(node) {
  return (
    node
    && node.callee
    && node.callee.property
    && node.callee.property.name === 'map'
  );
}

/**
 * Check whether given node is `ReturnStatement` of a React hook
 * @param {ASTNode} node The AST node
 * @param {Context} context eslint context
 * @returns {Boolean} True if node is a `ReturnStatement` of a React hook, false if not
 */
function isReturnStatementOfHook(node, context) {
  if (
    !node
    || !node.parent
    || node.parent.type !== 'ReturnStatement'
  ) {
    return false;
  }

  const callExpression = getClosestMatchingParent(node, context, isCallExpressionMatcher);
  return (
    callExpression
    && callExpression.callee
    && HOOK_REGEXP.test(callExpression.callee.name)
  );
}

/**
 * Check whether given node is declared inside a render prop
 * ```jsx
 * <Component renderFooter={() => <div />} />
 * <Component>{() => <div />}</Component>
 * ```
 * @param {ASTNode} node The AST node
 * @param {Context} context eslint context
 * @returns {Boolean} True if component is declared inside a render prop, false if not
 */
function isComponentInRenderProp(node, context) {
  if (
    node
    && node.parent
    && node.parent.type === 'Property'
    && node.parent.key
    && startsWithRender(node.parent.key.name)
  ) {
    return true;
  }

  // Check whether component is a render prop used as direct children, e.g. <Component>{() => <div />}</Component>
  if (
    node
    && node.parent
    && node.parent.type === 'JSXExpressionContainer'
    && node.parent.parent
    && node.parent.parent.type === 'JSXElement'
  ) {
    return true;
  }

  const jsxExpressionContainer = getClosestMatchingParent(node, context, isJSXExpressionContainerMatcher);

  // Check whether prop name indicates accepted patterns
  if (
    jsxExpressionContainer
    && jsxExpressionContainer.parent
    && jsxExpressionContainer.parent.type === 'JSXAttribute'
    && jsxExpressionContainer.parent.name
    && jsxExpressionContainer.parent.name.type === 'JSXIdentifier'
  ) {
    const propName = jsxExpressionContainer.parent.name.name;

    // Starts with render, e.g. <Component renderFooter={() => <div />} />
    if (startsWithRender(propName)) {
      return true;
    }

    // Uses children prop explicitly, e.g. <Component children={() => <div />} />
    if (propName === 'children') {
      return true;
    }
  }

  return false;
}

/**
 * Check whether given node is declared directly inside a render property
 * ```jsx
 * const rows = { render: () => <div /> }
 * <Component rows={ [{ render: () => <div /> }] } />
 *  ```
 * @param {ASTNode} node The AST node
 * @returns {Boolean} True if component is declared inside a render property, false if not
 */
function isDirectValueOfRenderProperty(node) {
  return (
    node
    && node.parent
    && node.parent.type === 'Property'
    && node.parent.key
    && node.parent.key.type === 'Identifier'
    && startsWithRender(node.parent.key.name)
  );
}

/**
 * Resolve the component name of given node
 * @param {ASTNode} node The AST node of the component
 * @returns {String} Name of the component, if any
 */
function resolveComponentName(node) {
  const parentName = node.id && node.id.name;
  if (parentName) return parentName;

  return (
    node.type === 'ArrowFunctionExpression'
    && node.parent
    && node.parent.id
    && node.parent.id.name
  );
}

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

module.exports = {
  meta: {
    docs: {
      description: 'Prevent creating unstable components inside components',
      category: 'Possible Errors',
      recommended: false,
      url: docsUrl('no-unstable-nested-components'),
    },
    schema: [{
      type: 'object',
      properties: {
        customValidators: {
          type: 'array',
          items: {
            type: 'string',
          },
        },
        allowAsProps: {
          type: 'boolean',
        },
      },
      additionalProperties: false,
    }],
  },

  create: Components.detect((context, components, utils) => {
    const allowAsProps = context.options.some((option) => option && option.allowAsProps);

    /**
     * Check whether given node is declared inside class component's render block
     * ```jsx
     * class Component extends React.Component {
     *   render() {
     *     class NestedClassComponent extends React.Component {
     * ...
     * ```
     * @param {ASTNode} node The AST node being checked
     * @returns {Boolean} True if node is inside class component's render block, false if not
     */
    function isInsideRenderMethod(node) {
      const parentComponent = utils.getParentComponent();

      if (!parentComponent || parentComponent.type !== 'ClassDeclaration') {
        return false;
      }

      return (
        node
        && node.parent
        && node.parent.type === 'MethodDefinition'
        && node.parent.key
        && node.parent.key.name === 'render'
      );
    }

    /**
     * Check whether given node is a function component declared inside class component.
     * Util's component detection fails to detect function components inside class components.
     * ```jsx
     * class Component extends React.Component {
     *  render() {
     *    const NestedComponent = () => <div />;
     * ...
     * ```
     * @param {ASTNode} node The AST node being checked
     * @returns {Boolean} True if given node a function component declared inside class component, false if not
     */
    function isFunctionComponentInsideClassComponent(node) {
      const parentComponent = utils.getParentComponent();
      const parentStatelessComponent = utils.getParentStatelessComponent();

      return (
        parentComponent
        && parentStatelessComponent
        && parentComponent.type === 'ClassDeclaration'
        && utils.getStatelessComponent(parentStatelessComponent)
        && utils.isReturningJSX(node)
      );
    }

    /**
     * Check whether given node is declared inside `createElement` call's props
     * ```js
     * React.createElement(Component, {
     *   footer: () => React.createElement("div", null)
     * })
     * ```
     * @param {ASTNode} node The AST node
     * @returns {Boolean} True if node is declare inside `createElement` call's props, false if not
     */
    function isComponentInsideCreateElementsProp(node) {
      if (!components.get(node)) {
        return false;
      }

      const createElementParent = getClosestMatchingParent(node, context, isCreateElementMatcher);

      return (
        createElementParent
        && createElementParent.arguments
        && createElementParent.arguments[1] === getClosestMatchingParent(node, context, isObjectExpressionMatcher)
      );
    }

    /**
     * Check whether given node is declared inside a component/object prop.
     * ```jsx
     * <Component footer={() => <div />} />
     * { footer: () => <div /> }
     * ```
     * @param {ASTNode} node The AST node being checked
     * @returns {Boolean} True if node is a component declared inside prop, false if not
     */
    function isComponentInProp(node) {
      if (isPropertyOfObjectExpressionMatcher(node)) {
        return utils.isReturningJSX(node);
      }

      const jsxAttribute = getClosestMatchingParent(node, context, isJSXAttributeOfExpressionContainerMatcher);

      if (!jsxAttribute) {
        return isComponentInsideCreateElementsProp(node);
      }

      return utils.isReturningJSX(node);
    }

    /**
     * Check whether given node is a stateless component returning non-JSX
     * ```jsx
     * {{ a: () => null }}
     * ```
     * @param {ASTNode} node The AST node being checked
     * @returns {Boolean} True if node is a stateless component returning non-JSX, false if not
     */
    function isStatelessComponentReturningNull(node) {
      const component = utils.getStatelessComponent(node);

      return component && !utils.isReturningJSX(component);
    }

    /**
     * Check whether given node is a unstable nested component
     * @param {ASTNode} node The AST node being checked
     */
    function validate(node) {
      if (!node || !node.parent) {
        return;
      }

      const isDeclaredInsideProps = isComponentInProp(node);

      if (
        !components.get(node)
        && !isFunctionComponentInsideClassComponent(node)
        && !isDeclaredInsideProps) {
        return;
      }

      if (
        // Support allowAsProps option
        (isDeclaredInsideProps && (allowAsProps || isComponentInRenderProp(node, context)))

        // Prevent reporting components created inside Array.map calls
        || isMapCall(node)
        || isMapCall(node.parent)

        // Do not mark components declared inside hooks (or falsly '() => null' clean-up methods)
        || isReturnStatementOfHook(node, context)

        // Do not mark objects containing render methods
        || isDirectValueOfRenderProperty(node)

        // Prevent reporting nested class components twice
        || isInsideRenderMethod(node)

        // Prevent falsely reporting deteceted "components" which do not return JSX
        || isStatelessComponentReturningNull(node)
      ) {
        return;
      }

      // Get the closest parent component
      const parentComponent = getClosestMatchingParent(
        node,
        context,
        (nodeToMatch) => components.get(nodeToMatch)
      );

      if (parentComponent) {
        const parentName = resolveComponentName(parentComponent);

        // Exclude lowercase parents, e.g. function createTestComponent()
        // React-dom prevents creating lowercase components
        if (parentName && parentName[0] === parentName[0].toLowerCase()) {
          return;
        }

        let message = generateErrorMessageWithParentName(parentName);

        // Add information about allowAsProps option when component is declared inside prop
        if (isDeclaredInsideProps && !allowAsProps) {
          message += COMPONENT_AS_PROPS_INFO;
        }

        report(context, message, null, {
          node,
        });
      }
    }

    // --------------------------------------------------------------------------
    // Public
    // --------------------------------------------------------------------------

    return {
      FunctionDeclaration(node) { validate(node); },
      ArrowFunctionExpression(node) { validate(node); },
      FunctionExpression(node) { validate(node); },
      ClassDeclaration(node) { validate(node); },
      CallExpression(node) { validate(node); },
    };
  }),
};
