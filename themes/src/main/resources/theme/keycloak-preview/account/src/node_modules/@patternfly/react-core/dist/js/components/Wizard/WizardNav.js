"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardNav = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardNav = function WizardNav(_ref) {
  var children = _ref.children,
      ariaLabel = _ref.ariaLabel,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$returnList = _ref.returnList,
      returnList = _ref$returnList === void 0 ? false : _ref$returnList;
  var innerList = React.createElement("ol", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardNavList)
  }, children);

  if (returnList) {
    return innerList;
  }

  return React.createElement("nav", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardNav, isOpen && 'pf-m-expanded'),
    "aria-label": ariaLabel
  }, React.createElement("ol", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardNavList)
  }, children));
};

exports.WizardNav = WizardNav;
WizardNav.propTypes = {
  children: _propTypes["default"].any,
  ariaLabel: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  returnList: _propTypes["default"].bool
};
//# sourceMappingURL=WizardNav.js.map