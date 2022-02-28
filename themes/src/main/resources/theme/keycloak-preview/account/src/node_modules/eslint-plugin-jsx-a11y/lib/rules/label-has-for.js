'use strict';

var _jsxAstUtils = require('jsx-ast-utils');

var _schemas = require('../util/schemas');

var _hasAccessibleChild = require('../util/hasAccessibleChild');

var _hasAccessibleChild2 = _interopRequireDefault(_hasAccessibleChild);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } } /**
                                                                                                                                                                                                     * @fileoverview Enforce label tags have htmlFor attribute.
                                                                                                                                                                                                     * @author Ethan Cohen
                                                                                                                                                                                                     */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var enumValues = ['nesting', 'id'];
var schema = {
  type: 'object',
  properties: {
    components: _schemas.arraySchema,
    required: {
      oneOf: [{ type: 'string', enum: enumValues }, (0, _schemas.generateObjSchema)({ some: (0, _schemas.enumArraySchema)(enumValues) }, ['some']), (0, _schemas.generateObjSchema)({ every: (0, _schemas.enumArraySchema)(enumValues) }, ['every'])]
    },
    allowChildren: { type: 'boolean' }
  }
};
// Breadth-first search, assuming that HTML for forms is shallow.
function validateNesting(node) {
  var queue = [].concat(_toConsumableArray(node.parent.children));
  var child = void 0;
  var opener = void 0;
  while (queue.length) {
    child = queue.shift();
    opener = child.openingElement;
    if (child.type === 'JSXElement' && opener && (opener.name.name === 'input' || opener.name.name === 'textarea')) {
      return true;
    }
    if (child.children) {
      queue = queue.concat(child.children);
    }
  }
  return false;
}

var validateId = function validateId(node) {
  var htmlForAttr = (0, _jsxAstUtils.getProp)(node.attributes, 'htmlFor');
  var htmlForValue = (0, _jsxAstUtils.getPropValue)(htmlForAttr);

  return htmlForAttr !== false && !!htmlForValue;
};

var validate = function validate(node, required, allowChildren) {
  if (allowChildren === true) {
    return (0, _hasAccessibleChild2.default)(node.parent);
  }
  if (required === 'nesting') {
    return validateNesting(node);
  }
  return validateId(node);
};

var getValidityStatus = function getValidityStatus(node, required, allowChildren) {
  if (Array.isArray(required.some)) {
    var _isValid = required.some.some(function (rule) {
      return validate(node, rule, allowChildren);
    });
    var _message = !_isValid ? 'Form label must have ANY of the following types of associated control: ' + required.some.join(', ') : null;
    return { isValid: _isValid, message: _message };
  }
  if (Array.isArray(required.every)) {
    var _isValid2 = required.every.every(function (rule) {
      return validate(node, rule, allowChildren);
    });
    var _message2 = !_isValid2 ? 'Form label must have ALL of the following types of associated control: ' + required.every.join(', ') : null;
    return { isValid: _isValid2, message: _message2 };
  }

  var isValid = validate(node, required, allowChildren);
  var message = !isValid ? 'Form label must have the following type of associated control: ' + required : null;
  return { isValid: isValid, message: message };
};

module.exports = {
  meta: {
    deprecated: true,
    docs: {
      url: 'https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules/label-has-for.md'
    },
    schema: [schema]
  },

  create: function create(context) {
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var options = context.options[0] || {};
        var componentOptions = options.components || [];
        var typesToValidate = ['label'].concat(componentOptions);
        var nodeType = (0, _jsxAstUtils.elementType)(node);

        // Only check 'label' elements and custom types.
        if (typesToValidate.indexOf(nodeType) === -1) {
          return;
        }

        var required = options.required || { every: ['nesting', 'id'] };
        var allowChildren = options.allowChildren || false;

        var _getValidityStatus = getValidityStatus(node, required, allowChildren),
            isValid = _getValidityStatus.isValid,
            message = _getValidityStatus.message;

        if (!isValid) {
          context.report({
            node: node,
            message: message
          });
        }
      }
    };
  }
};