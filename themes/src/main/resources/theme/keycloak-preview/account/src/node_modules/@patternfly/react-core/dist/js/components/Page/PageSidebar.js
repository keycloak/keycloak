"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.PageSidebar = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _page = _interopRequireDefault(require("@patternfly/react-styles/css/components/Page/page"));

var _reactStyles = require("@patternfly/react-styles");

var _Page = require("./Page");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var PageSidebar = function PageSidebar(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      nav = _ref.nav,
      _ref$isNavOpen = _ref.isNavOpen,
      isNavOpen = _ref$isNavOpen === void 0 ? true : _ref$isNavOpen,
      _ref$theme = _ref.theme,
      theme = _ref$theme === void 0 ? 'light' : _ref$theme,
      props = _objectWithoutProperties(_ref, ["className", "nav", "isNavOpen", "theme"]);

  return React.createElement(_Page.PageContextConsumer, null, function (_ref2) {
    var isManagedSidebar = _ref2.isManagedSidebar,
        managedIsNavOpen = _ref2.isNavOpen;
    var navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
    return React.createElement("div", _extends({
      id: "page-sidebar",
      className: (0, _reactStyles.css)(_page["default"].pageSidebar, theme === 'dark' && _page["default"].modifiers.dark, navOpen && _page["default"].modifiers.expanded, !navOpen && _page["default"].modifiers.collapsed, className)
    }, props), React.createElement("div", {
      className: (0, _reactStyles.css)(_page["default"].pageSidebarBody)
    }, nav));
  });
};

exports.PageSidebar = PageSidebar;
PageSidebar.propTypes = {
  className: _propTypes["default"].string,
  nav: _propTypes["default"].node,
  isManagedSidebar: _propTypes["default"].bool,
  isNavOpen: _propTypes["default"].bool,
  theme: _propTypes["default"].oneOf(['dark', 'light'])
};
//# sourceMappingURL=PageSidebar.js.map