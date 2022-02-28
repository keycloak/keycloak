'use strict';

var _jsxAstUtils = require('jsx-ast-utils');

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

var _has = require('has');

var _has2 = _interopRequireDefault(_has);

var _getExplicitRole = require('../util/getExplicitRole');

var _getExplicitRole2 = _interopRequireDefault(_getExplicitRole);

var _getImplicitRole = require('../util/getImplicitRole');

var _getImplicitRole2 = _interopRequireDefault(_getImplicitRole);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var errorMessage = function errorMessage(element, implicitRole) {
  return 'The element ' + element + ' has an implicit role of ' + implicitRole + '. Defining this explicitly is redundant and should be avoided.';
}; /**
    * @fileoverview Enforce explicit role property is not the
    * same as implicit/default role property on element.
    * @author Ethan Cohen <@evcohen>
    * 
    */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var DEFAULT_ROLE_EXCEPTIONS = { nav: ['navigation'] };

module.exports = {
  meta: {
    docs: {
      url: 'https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules/no-redundant-roles.md'
    },
    schema: [{
      type: 'object',
      additionalProperties: {
        type: 'array',
        items: {
          type: 'string'
        },
        uniqueItems: true
      }
    }]
  },

  create: function create(context) {
    var options = context.options;

    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var type = (0, _jsxAstUtils.elementType)(node);
        var implicitRole = (0, _getImplicitRole2.default)(type, node.attributes);
        var explicitRole = (0, _getExplicitRole2.default)(type, node.attributes);

        if (!implicitRole || !explicitRole) {
          return;
        }

        if (implicitRole === explicitRole) {
          var allowedRedundantRoles = options[0] || {};
          var redundantRolesForElement = void 0;

          if ((0, _has2.default)(allowedRedundantRoles, type)) {
            redundantRolesForElement = allowedRedundantRoles[type];
          } else {
            redundantRolesForElement = DEFAULT_ROLE_EXCEPTIONS[type] || [];
          }

          if ((0, _arrayIncludes2.default)(redundantRolesForElement, implicitRole)) {
            return;
          }

          context.report({
            node: node,
            message: errorMessage(type, implicitRole.toLowerCase())
          });
        }
      }
    };
  }
};