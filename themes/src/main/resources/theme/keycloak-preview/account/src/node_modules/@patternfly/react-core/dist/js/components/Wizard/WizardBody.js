"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardBody = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardBody = function WizardBody(_ref) {
  var children = _ref.children,
      _ref$hasBodyPadding = _ref.hasBodyPadding,
      hasBodyPadding = _ref$hasBodyPadding === void 0 ? true : _ref$hasBodyPadding;
  return React.createElement("main", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardMain, !hasBodyPadding && _wizard["default"].modifiers.noPadding)
  }, React.createElement("div", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardMainBody)
  }, children));
};

exports.WizardBody = WizardBody;
WizardBody.propTypes = {
  children: _propTypes["default"].any.isRequired,
  hasBodyPadding: _propTypes["default"].bool.isRequired
};
//# sourceMappingURL=WizardBody.js.map