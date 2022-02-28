"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ApplicationLauncherContent = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _appLauncher = _interopRequireDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));

var _accessibility = _interopRequireDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));

var _ApplicationLauncherIcon = require("./ApplicationLauncherIcon");

var _ApplicationLauncherText = require("./ApplicationLauncherText");

var _externalLinkAltIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/external-link-alt-icon"));

var _ApplicationLauncherItemContext = require("./ApplicationLauncherItemContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ApplicationLauncherContent = function ApplicationLauncherContent(_ref) {
  var children = _ref.children;
  return React.createElement(_ApplicationLauncherItemContext.ApplicationLauncherItemContext.Consumer, null, function (_ref2) {
    var isExternal = _ref2.isExternal,
        icon = _ref2.icon;
    return React.createElement(React.Fragment, null, icon && React.createElement(_ApplicationLauncherIcon.ApplicationLauncherIcon, null, icon), icon ? React.createElement(_ApplicationLauncherText.ApplicationLauncherText, null, children) : children, isExternal && React.createElement(React.Fragment, null, React.createElement("span", {
      className: (0, _reactStyles.css)(_appLauncher["default"].appLauncherMenuItemExternalIcon)
    }, React.createElement(_externalLinkAltIcon["default"], null)), React.createElement("span", {
      className: (0, _reactStyles.css)(_accessibility["default"].screenReader)
    }, "(opens new window)")));
  });
};

exports.ApplicationLauncherContent = ApplicationLauncherContent;
ApplicationLauncherContent.propTypes = {
  children: _propTypes["default"].node.isRequired
};
//# sourceMappingURL=ApplicationLauncherContent.js.map