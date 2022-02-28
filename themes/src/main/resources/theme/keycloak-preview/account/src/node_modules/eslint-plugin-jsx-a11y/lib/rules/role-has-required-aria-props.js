'use strict';

var _ariaQuery = require('aria-query');

var _jsxAstUtils = require('jsx-ast-utils');

var _schemas = require('../util/schemas');

var _isSemanticRoleElement = require('../util/isSemanticRoleElement');

var _isSemanticRoleElement2 = _interopRequireDefault(_isSemanticRoleElement);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } } /**
                                                                                                                                                                                                     * @fileoverview Enforce that elements with ARIA roles must
                                                                                                                                                                                                     *  have all required attributes for that role.
                                                                                                                                                                                                     * @author Ethan Cohen
                                                                                                                                                                                                     */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var errorMessage = function errorMessage(role, requiredProps) {
  return 'Elements with the ARIA role "' + role + '" must have the following attributes defined: ' + String(requiredProps).toLowerCase();
};

var schema = (0, _schemas.generateObjSchema)();

module.exports = {
  meta: {
    docs: {
      url: 'https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules/role-has-required-aria-props.md'
    },
    schema: [schema]
  },

  create: function create(context) {
    return {
      JSXAttribute: function JSXAttribute(attribute) {
        var name = (0, _jsxAstUtils.propName)(attribute).toLowerCase();

        if (name !== 'role') {
          return;
        }

        var type = (0, _jsxAstUtils.elementType)(attribute.parent);
        if (!_ariaQuery.dom.get(type)) {
          return;
        }

        var roleAttrValue = (0, _jsxAstUtils.getLiteralPropValue)(attribute);
        var attributes = attribute.parent.attributes;

        // If value is undefined, then the role attribute will be dropped in the DOM.
        // If value is null, then getLiteralAttributeValue is telling us
        // that the value isn't in the form of a literal.

        if (roleAttrValue === undefined || roleAttrValue === null) {
          return;
        }

        var normalizedValues = String(roleAttrValue).toLowerCase().split(' ');
        var validRoles = normalizedValues.filter(function (val) {
          return [].concat(_toConsumableArray(_ariaQuery.roles.keys())).indexOf(val) > -1;
        });

        // Check semantic DOM elements
        // For example, <input type="checkbox" role="switch" />
        if ((0, _isSemanticRoleElement2.default)(type, attributes)) {
          return;
        }
        // Check arbitrary DOM elements
        validRoles.forEach(function (role) {
          var _roles$get = _ariaQuery.roles.get(role),
              requiredPropKeyValues = _roles$get.requiredProps;

          var requiredProps = Object.keys(requiredPropKeyValues);

          if (requiredProps.length > 0) {
            var hasRequiredProps = requiredProps.every(function (prop) {
              return (0, _jsxAstUtils.getProp)(attribute.parent.attributes, prop);
            });
            if (hasRequiredProps === false) {
              context.report({
                node: attribute,
                message: errorMessage(role.toLowerCase(), requiredProps)
              });
            }
          }
        });
      }
    };
  }
};