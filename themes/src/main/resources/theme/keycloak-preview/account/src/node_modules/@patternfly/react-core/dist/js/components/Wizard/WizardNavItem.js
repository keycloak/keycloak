"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardNavItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardNavItem = function WizardNavItem(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$text = _ref.text,
      text = _ref$text === void 0 ? '' : _ref$text,
      _ref$isCurrent = _ref.isCurrent,
      isCurrent = _ref$isCurrent === void 0 ? false : _ref$isCurrent,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      step = _ref.step,
      _ref$onNavItemClick = _ref.onNavItemClick,
      onNavItemClick = _ref$onNavItemClick === void 0 ? function () {
    return undefined;
  } : _ref$onNavItemClick,
      _ref$navItemComponent = _ref.navItemComponent,
      navItemComponent = _ref$navItemComponent === void 0 ? 'a' : _ref$navItemComponent;
  var NavItemComponent = navItemComponent;
  return React.createElement("li", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardNavItem)
  }, React.createElement(NavItemComponent, {
    "aria-current": isCurrent && !children ? 'page' : false,
    onClick: function onClick() {
      return onNavItemClick(step);
    },
    className: (0, _reactStyles.css)(_wizard["default"].wizardNavLink, isCurrent && 'pf-m-current', isDisabled && 'pf-m-disabled'),
    "aria-disabled": isDisabled ? true : false,
    tabIndex: isDisabled ? -1 : undefined
  }, text), children);
};

exports.WizardNavItem = WizardNavItem;
WizardNavItem.propTypes = {
  children: _propTypes["default"].node,
  text: _propTypes["default"].string,
  isCurrent: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  step: _propTypes["default"].number.isRequired,
  onNavItemClick: _propTypes["default"].func,
  navItemComponent: _propTypes["default"].node
};
//# sourceMappingURL=WizardNavItem.js.map