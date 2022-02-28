/**
 * @fileoverview Common used propTypes detection functionality.
 */
'use strict';

const astUtil = require('./ast');
const versionUtil = require('./version');

// ------------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------------

const DIRECT_PROPS_REGEX = /^props\s*(\.|\[)/;
const DIRECT_NEXT_PROPS_REGEX = /^nextProps\s*(\.|\[)/;
const DIRECT_PREV_PROPS_REGEX = /^prevProps\s*(\.|\[)/;
const LIFE_CYCLE_METHODS = ['componentWillReceiveProps', 'shouldComponentUpdate', 'componentWillUpdate', 'componentDidUpdate'];
const ASYNC_SAFE_LIFE_CYCLE_METHODS = ['getDerivedStateFromProps', 'getSnapshotBeforeUpdate', 'UNSAFE_componentWillReceiveProps', 'UNSAFE_componentWillUpdate'];

/**
 * Checks if a prop init name matches common naming patterns
 * @param {ASTNode} node The AST node being checked.
 * @returns {Boolean} True if the prop name matches
 */
function isPropAttributeName (node) {
  return (
    node.init.name === 'props' ||
    node.init.name === 'nextProps' ||
    node.init.name === 'prevProps'
  );
}

/**
 * Checks if the component must be validated
 * @param {Object} component The component to process
 * @returns {Boolean} True if the component must be validated, false if not.
 */
function mustBeValidated(component) {
  return !!(component && !component.ignorePropsValidation);
}

module.exports = function usedPropTypesInstructions(context, components, utils) {
  const sourceCode = context.getSourceCode();
  const checkAsyncSafeLifeCycles = versionUtil.testReactVersion(context, '16.3.0');

  /**
   * Check if we are in a class constructor
   * @return {boolean} true if we are in a class constructor, false if not
   */
  function inComponentWillReceiveProps() {
    let scope = context.getScope();
    while (scope) {
      if (
        scope.block
        && scope.block.parent
        && scope.block.parent.key
        && scope.block.parent.key.name === 'componentWillReceiveProps'
      ) {
        return true;
      }
      scope = scope.upper;
    }
    return false;
  }

  /**
   * Check if we are in a lifecycle method
   * @return {boolean} true if we are in a class constructor, false if not
   **/
  function inLifeCycleMethod() {
    let scope = context.getScope();
    while (scope) {
      if (scope.block && scope.block.parent && scope.block.parent.key) {
        const name = scope.block.parent.key.name;

        if (LIFE_CYCLE_METHODS.indexOf(name) >= 0) {
          return true;
        }
        if (checkAsyncSafeLifeCycles && ASYNC_SAFE_LIFE_CYCLE_METHODS.indexOf(name) >= 0) {
          return true;
        }
      }
      scope = scope.upper;
    }
    return false;
  }

  /**
   * Returns true if the given node is a React Component lifecycle method
   * @param {ASTNode} node The AST node being checked.
   * @return {Boolean} True if the node is a lifecycle method
   */
  function isNodeALifeCycleMethod(node) {
    const nodeKeyName = (node.key || {}).name;

    if (node.kind === 'constructor') {
      return true;
    }
    if (LIFE_CYCLE_METHODS.indexOf(nodeKeyName) >= 0) {
      return true;
    }
    if (checkAsyncSafeLifeCycles && ASYNC_SAFE_LIFE_CYCLE_METHODS.indexOf(nodeKeyName) >= 0) {
      return true;
    }

    return false;
  }

  /**
   * Returns true if the given node is inside a React Component lifecycle
   * method.
   * @param {ASTNode} node The AST node being checked.
   * @return {Boolean} True if the node is inside a lifecycle method
   */
  function isInLifeCycleMethod(node) {
    if ((node.type === 'MethodDefinition' || node.type === 'Property') && isNodeALifeCycleMethod(node)) {
      return true;
    }

    if (node.parent) {
      return isInLifeCycleMethod(node.parent);
    }

    return false;
  }

  /**
   * Check if the current node is in a setState updater method
   * @return {boolean} true if we are in a setState updater, false if not
   */
  function inSetStateUpdater() {
    let scope = context.getScope();
    while (scope) {
      if (
        scope.block && scope.block.parent
        && scope.block.parent.type === 'CallExpression'
        && scope.block.parent.callee.property
        && scope.block.parent.callee.property.name === 'setState'
        // Make sure we are in the updater not the callback
        && scope.block.parent.arguments[0].start === scope.block.start
      ) {
        return true;
      }
      scope = scope.upper;
    }
    return false;
  }

  function isPropArgumentInSetStateUpdater(node) {
    let scope = context.getScope();
    while (scope) {
      if (
        scope.block && scope.block.parent
        && scope.block.parent.type === 'CallExpression'
        && scope.block.parent.callee.property
        && scope.block.parent.callee.property.name === 'setState'
        // Make sure we are in the updater not the callback
        && scope.block.parent.arguments[0].start === scope.block.start
        && scope.block.parent.arguments[0].params
        && scope.block.parent.arguments[0].params.length > 1
      ) {
        return scope.block.parent.arguments[0].params[1].name === node.object.name;
      }
      scope = scope.upper;
    }
    return false;
  }

  /**
   * Checks if the prop has spread operator.
   * @param {ASTNode} node The AST node being marked.
   * @returns {Boolean} True if the prop has spread operator, false if not.
   */
  function hasSpreadOperator(node) {
    const tokens = sourceCode.getTokens(node);
    return tokens.length && tokens[0].value === '...';
  }

  /**
   * Removes quotes from around an identifier.
   * @param {string} the identifier to strip
   */
  function stripQuotes(string) {
    return string.replace(/^\'|\'$/g, '');
  }

  /**
   * Retrieve the name of a key node
   * @param {ASTNode} node The AST node with the key.
   * @return {string} the name of the key
   */
  function getKeyValue(node) {
    if (node.type === 'ObjectTypeProperty') {
      const tokens = context.getFirstTokens(node, 2);
      return (tokens[0].value === '+' || tokens[0].value === '-'
        ? tokens[1].value
        : stripQuotes(tokens[0].value)
      );
    }
    const key = node.key || node.argument;
    return key.type === 'Identifier' ? key.name : key.value;
  }

  /**
   * Check if we are in a class constructor
   * @return {boolean} true if we are in a class constructor, false if not
   */
  function inConstructor() {
    let scope = context.getScope();
    while (scope) {
      if (scope.block && scope.block.parent && scope.block.parent.kind === 'constructor') {
        return true;
      }
      scope = scope.upper;
    }
    return false;
  }

  /**
   * Retrieve the name of a property node
   * @param {ASTNode} node The AST node with the property.
   * @return {string} the name of the property or undefined if not found
   */
  function getPropertyName(node) {
    const isDirectProp = DIRECT_PROPS_REGEX.test(sourceCode.getText(node));
    const isDirectNextProp = DIRECT_NEXT_PROPS_REGEX.test(sourceCode.getText(node));
    const isDirectPrevProp = DIRECT_PREV_PROPS_REGEX.test(sourceCode.getText(node));
    const isDirectSetStateProp = isPropArgumentInSetStateUpdater(node);
    const isInClassComponent = utils.getParentES6Component() || utils.getParentES5Component();
    const isNotInConstructor = !inConstructor(node);
    const isNotInLifeCycleMethod = !inLifeCycleMethod();
    const isNotInSetStateUpdater = !inSetStateUpdater();
    if ((isDirectProp || isDirectNextProp || isDirectPrevProp || isDirectSetStateProp)
      && isInClassComponent
      && isNotInConstructor
      && isNotInLifeCycleMethod
      && isNotInSetStateUpdater
    ) {
      return void 0;
    }
    if (!isDirectProp && !isDirectNextProp && !isDirectPrevProp && !isDirectSetStateProp) {
      node = node.parent;
    }
    const property = node.property;
    if (property) {
      switch (property.type) {
        case 'Identifier':
          if (node.computed) {
            return '__COMPUTED_PROP__';
          }
          return property.name;
        case 'MemberExpression':
          return void 0;
        case 'Literal':
          // Accept computed properties that are literal strings
          if (typeof property.value === 'string') {
            return property.value;
          }
          // falls through
        default:
          if (node.computed) {
            return '__COMPUTED_PROP__';
          }
          break;
      }
    }
    return void 0;
  }

  /**
   * Checks if a prop is being assigned a value props.bar = 'bar'
   * @param {ASTNode} node The AST node being checked.
   * @returns {Boolean}
   */
  function isAssignmentToProp(node) {
    return (
      node.parent &&
      node.parent.type === 'AssignmentExpression' &&
      node.parent.left === node
    );
  }

  /**
   * Checks if we are using a prop
   * @param {ASTNode} node The AST node being checked.
   * @returns {Boolean} True if we are using a prop, false if not.
   */
  function isPropTypesUsage(node) {
    const isThisPropsUsage = node.object.type === 'ThisExpression' && node.property.name === 'props';
    const isPropsUsage = isThisPropsUsage || node.object.name === 'nextProps' || node.object.name === 'prevProps';
    const isClassUsage = (
      (utils.getParentES6Component() || utils.getParentES5Component()) &&
      (isThisPropsUsage || isPropArgumentInSetStateUpdater(node))
    );
    const isStatelessFunctionUsage = node.object.name === 'props' && !isAssignmentToProp(node);
    return isClassUsage || isStatelessFunctionUsage || (isPropsUsage && inLifeCycleMethod());
  }

  /**
   * Mark a prop type as used
   * @param {ASTNode} node The AST node being marked.
   */
  function markPropTypesAsUsed(node, parentNames) {
    parentNames = parentNames || [];
    let type;
    let name;
    let allNames;
    let properties;
    switch (node.type) {
      case 'MemberExpression':
        name = getPropertyName(node);
        if (name) {
          allNames = parentNames.concat(name);
          if (node.parent.type === 'MemberExpression') {
            markPropTypesAsUsed(node.parent, allNames);
          }
          // Do not mark computed props as used.
          type = name !== '__COMPUTED_PROP__' ? 'direct' : null;
        } else if (
          node.parent.id &&
          node.parent.id.properties &&
          node.parent.id.properties.length &&
          getKeyValue(node.parent.id.properties[0])
        ) {
          type = 'destructuring';
          properties = node.parent.id.properties;
        }
        break;
      case 'ArrowFunctionExpression':
      case 'FunctionDeclaration':
      case 'FunctionExpression':
        if (node.params.length === 0) {
          break;
        }
        type = 'destructuring';
        properties = node.params[0].properties;
        if (inSetStateUpdater()) {
          properties = node.params[1].properties;
        }
        break;
      case 'VariableDeclarator':
        for (let i = 0, j = node.id.properties.length; i < j; i++) {
          // let {props: {firstname}} = this
          const thisDestructuring = (
            node.id.properties[i].key && (
              (node.id.properties[i].key.name === 'props' || node.id.properties[i].key.value === 'props') &&
              node.id.properties[i].value.type === 'ObjectPattern'
            )
          );
          // let {firstname} = props
          const genericDestructuring = isPropAttributeName(node) && (
            utils.getParentStatelessComponent() ||
            isInLifeCycleMethod(node)
          );

          if (thisDestructuring) {
            properties = node.id.properties[i].value.properties;
          } else if (genericDestructuring) {
            properties = node.id.properties;
          } else {
            continue;
          }
          type = 'destructuring';
          break;
        }
        break;
      default:
        throw new Error(`${node.type} ASTNodes are not handled by markPropTypesAsUsed`);
    }

    const component = components.get(utils.getParentComponent());
    const usedPropTypes = component && component.usedPropTypes || [];
    let ignoreUnusedPropTypesValidation = component && component.ignoreUnusedPropTypesValidation || false;

    switch (type) {
      case 'direct':
        // Ignore Object methods
        if (name in Object.prototype) {
          break;
        }

        const nodeSource = sourceCode.getText(node);
        const isDirectProp = DIRECT_PROPS_REGEX.test(nodeSource)
          || DIRECT_NEXT_PROPS_REGEX.test(nodeSource)
          || DIRECT_PREV_PROPS_REGEX.test(nodeSource);
        const reportedNode = (
          !isDirectProp && !inConstructor() && !inComponentWillReceiveProps() ?
            node.parent.property :
            node.property
        );
        usedPropTypes.push({
          name: name,
          allNames: allNames,
          node: reportedNode
        });
        break;
      case 'destructuring':
        for (let k = 0, l = (properties || []).length; k < l; k++) {
          if (hasSpreadOperator(properties[k]) || properties[k].computed) {
            ignoreUnusedPropTypesValidation = true;
            break;
          }
          const propName = getKeyValue(properties[k]);

          let currentNode = node;
          allNames = [];
          while (currentNode.property && currentNode.property.name !== 'props') {
            allNames.unshift(currentNode.property.name);
            currentNode = currentNode.object;
          }
          allNames.push(propName);
          if (propName) {
            usedPropTypes.push({
              allNames: allNames,
              name: propName,
              node: properties[k]
            });
          }
        }
        break;
      default:
        break;
    }

    components.set(component ? component.node : node, {
      usedPropTypes: usedPropTypes,
      ignoreUnusedPropTypesValidation: ignoreUnusedPropTypesValidation
    });
  }

  /**
   * @param {ASTNode} node We expect either an ArrowFunctionExpression,
   *   FunctionDeclaration, or FunctionExpression
   */
  function markDestructuredFunctionArgumentsAsUsed(node) {
    const destructuring = node.params && node.params[0] && node.params[0].type === 'ObjectPattern';
    if (destructuring && (components.get(node) || components.get(node.parent))) {
      markPropTypesAsUsed(node);
    }
  }

  function handleSetStateUpdater(node) {
    if (!node.params || node.params.length < 2 || !inSetStateUpdater()) {
      return;
    }
    markPropTypesAsUsed(node);
  }

  /**
   * Handle both stateless functions and setState updater functions.
   * @param {ASTNode} node We expect either an ArrowFunctionExpression,
   *   FunctionDeclaration, or FunctionExpression
   */
  function handleFunctionLikeExpressions(node) {
    handleSetStateUpdater(node);
    markDestructuredFunctionArgumentsAsUsed(node);
  }

  function handleCustomValidators(component) {
    const propTypes = component.declaredPropTypes;
    if (!propTypes) {
      return;
    }

    Object.keys(propTypes).forEach(key => {
      const node = propTypes[key].node;

      if (node.value && astUtil.isFunctionLikeExpression(node.value)) {
        markPropTypesAsUsed(node.value);
      }
    });
  }

  return {
    VariableDeclarator: function(node) {
      const destructuring = node.init && node.id && node.id.type === 'ObjectPattern';
      // let {props: {firstname}} = this
      const thisDestructuring = destructuring && node.init.type === 'ThisExpression';
      // let {firstname} = props
      const statelessDestructuring = destructuring && isPropAttributeName(node) && (
        utils.getParentStatelessComponent() ||
        isInLifeCycleMethod(node)
      );

      if (!thisDestructuring && !statelessDestructuring) {
        return;
      }
      markPropTypesAsUsed(node);
    },

    FunctionDeclaration: handleFunctionLikeExpressions,

    ArrowFunctionExpression: handleFunctionLikeExpressions,

    FunctionExpression: handleFunctionLikeExpressions,

    JSXSpreadAttribute: function(node) {
      const component = components.get(utils.getParentComponent());
      components.set(component ? component.node : node, {
        ignoreUnusedPropTypesValidation: true
      });
    },

    MemberExpression: function(node) {
      if (isPropTypesUsage(node)) {
        markPropTypesAsUsed(node);
      }
    },

    ObjectPattern: function(node) {
      // If the object pattern is a destructured props object in a lifecycle
      // method -- mark it for used props.
      if (isNodeALifeCycleMethod(node.parent.parent) && node.properties.length > 0) {
        markPropTypesAsUsed(node.parent);
      }
    },

    'Program:exit': function() {
      const list = components.list();

      Object.keys(list).filter(component => mustBeValidated(list[component])).forEach(component => {
        handleCustomValidators(list[component]);
      });
    }
  };
};
