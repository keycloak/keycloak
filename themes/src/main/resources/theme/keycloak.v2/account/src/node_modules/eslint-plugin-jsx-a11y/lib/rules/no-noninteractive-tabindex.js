"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _defineProperty2 = _interopRequireDefault(require("@babel/runtime/helpers/defineProperty"));

var _ariaQuery = require("aria-query");

var _jsxAstUtils = require("jsx-ast-utils");

var _arrayIncludes = _interopRequireDefault(require("array-includes"));

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _isInteractiveElement = _interopRequireDefault(require("../util/isInteractiveElement"));

var _isInteractiveRole = _interopRequireDefault(require("../util/isInteractiveRole"));

var _isNonLiteralProperty = _interopRequireDefault(require("../util/isNonLiteralProperty"));

var _schemas = require("../util/schemas");

var _getTabIndex = _interopRequireDefault(require("../util/getTabIndex"));

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); enumerableOnly && (symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; })), keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = null != arguments[i] ? arguments[i] : {}; i % 2 ? ownKeys(Object(source), !0).forEach(function (key) { (0, _defineProperty2["default"])(target, key, source[key]); }) : Object.getOwnPropertyDescriptors ? Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)) : ownKeys(Object(source)).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } return target; }

var errorMessage = '`tabIndex` should only be declared on interactive elements.';
var schema = (0, _schemas.generateObjSchema)({
  roles: _objectSpread(_objectSpread({}, _schemas.arraySchema), {}, {
    description: 'An array of ARIA roles'
  }),
  tags: _objectSpread(_objectSpread({}, _schemas.arraySchema), {}, {
    description: 'An array of HTML tag names'
  })
});
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/no-noninteractive-tabindex.md',
      description: '`tabIndex` should only be declared on interactive elements.',
      errorOptions: true
    },
    schema: [schema]
  },
  create: function create(context) {
    var options = context.options;
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var type = elementType(node);
        var attributes = node.attributes;
        var tabIndexProp = (0, _jsxAstUtils.getProp)(attributes, 'tabIndex');
        var tabIndex = (0, _getTabIndex["default"])(tabIndexProp); // Early return;

        if (typeof tabIndex === 'undefined') {
          return;
        }

        var role = (0, _jsxAstUtils.getLiteralPropValue)((0, _jsxAstUtils.getProp)(node.attributes, 'role'));

        if (!_ariaQuery.dom.has(type)) {
          // Do not test higher level JSX components, as we do not know what
          // low-level DOM element this maps to.
          return;
        } // Allow for configuration overrides.


        var _ref = options[0] || {},
            tags = _ref.tags,
            roles = _ref.roles,
            allowExpressionValues = _ref.allowExpressionValues;

        if (tags && (0, _arrayIncludes["default"])(tags, type)) {
          return;
        }

        if (roles && (0, _arrayIncludes["default"])(roles, role)) {
          return;
        }

        if (allowExpressionValues === true && (0, _isNonLiteralProperty["default"])(attributes, 'role')) {
          // Special case if role is assigned using ternary with literals on both side
          var roleProp = (0, _jsxAstUtils.getProp)(attributes, 'role');

          if (roleProp && roleProp.type === 'JSXAttribute' && roleProp.value.type === 'JSXExpressionContainer') {
            if (roleProp.value.expression.type === 'ConditionalExpression') {
              if (roleProp.value.expression.consequent.type === 'Literal' && roleProp.value.expression.alternate.type === 'Literal') {
                return;
              }
            }
          }

          return;
        }

        if ((0, _isInteractiveElement["default"])(type, attributes) || (0, _isInteractiveRole["default"])(type, attributes)) {
          return;
        }

        if (tabIndex >= 0) {
          context.report({
            node: tabIndexProp,
            message: errorMessage
          });
        }
      }
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;