'use strict';

var _jsxAstUtils = require('jsx-ast-utils');

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

var _schemas = require('../util/schemas');

var _isDOMElement = require('../util/isDOMElement');

var _isDOMElement2 = _interopRequireDefault(_isDOMElement);

var _isInteractiveElement = require('../util/isInteractiveElement');

var _isInteractiveElement2 = _interopRequireDefault(_isInteractiveElement);

var _isInteractiveRole = require('../util/isInteractiveRole');

var _isInteractiveRole2 = _interopRequireDefault(_isInteractiveRole);

var _mayHaveAccessibleLabel = require('../util/mayHaveAccessibleLabel');

var _mayHaveAccessibleLabel2 = _interopRequireDefault(_mayHaveAccessibleLabel);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var errorMessage = 'A control must be associated with a text label.'; /**
                                                                       * @fileoverview Enforce controls are associated with a text label.
                                                                       * @author Jesse Beach
                                                                       *
                                                                       * 
                                                                       */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var schema = (0, _schemas.generateObjSchema)({
  labelAttributes: _schemas.arraySchema,
  controlComponents: _schemas.arraySchema,
  ignoreElements: _schemas.arraySchema,
  ignoreRoles: _schemas.arraySchema,
  depth: {
    description: 'JSX tree depth limit to check for accessible label',
    type: 'integer',
    minimum: 0
  }
});

module.exports = {
  meta: {
    docs: {},
    schema: [schema]
  },

  create: function create(context) {
    var options = context.options[0] || {};
    var _options$labelAttribu = options.labelAttributes,
        labelAttributes = _options$labelAttribu === undefined ? [] : _options$labelAttribu,
        _options$controlCompo = options.controlComponents,
        controlComponents = _options$controlCompo === undefined ? [] : _options$controlCompo,
        _options$ignoreElemen = options.ignoreElements,
        ignoreElements = _options$ignoreElemen === undefined ? [] : _options$ignoreElemen,
        _options$ignoreRoles = options.ignoreRoles,
        ignoreRoles = _options$ignoreRoles === undefined ? [] : _options$ignoreRoles;


    var rule = function rule(node) {
      var tag = (0, _jsxAstUtils.elementType)(node.openingElement);
      var role = (0, _jsxAstUtils.getLiteralPropValue)((0, _jsxAstUtils.getProp)(node.openingElement.attributes, 'role'));
      // Ignore interactive elements that might get their label from a source
      // that cannot be discerned from static analysis, like
      // <label><input />Save</label>
      if ((0, _arrayIncludes2.default)(ignoreElements, tag)) {
        return;
      }
      // Ignore roles that are "interactive" but should not require a label.
      if ((0, _arrayIncludes2.default)(ignoreRoles, role)) {
        return;
      }
      var props = node.openingElement.attributes;
      var nodeIsDOMElement = (0, _isDOMElement2.default)(tag);
      var nodeIsInteractiveElement = (0, _isInteractiveElement2.default)(tag, props);
      var nodeIsInteractiveRole = (0, _isInteractiveRole2.default)(tag, props);
      var nodeIsControlComponent = controlComponents.indexOf(tag) > -1;

      var hasAccessibleLabel = true;
      if (nodeIsInteractiveElement || nodeIsDOMElement && nodeIsInteractiveRole || nodeIsControlComponent) {
        // Prevent crazy recursion.
        var recursionDepth = Math.min(options.depth === undefined ? 2 : options.depth, 25);
        hasAccessibleLabel = (0, _mayHaveAccessibleLabel2.default)(node, recursionDepth, labelAttributes);
      }

      if (!hasAccessibleLabel) {
        context.report({
          node: node.openingElement,
          message: errorMessage
        });
      }
    };

    // Create visitor selectors.
    return {
      JSXElement: rule
    };
  }
};