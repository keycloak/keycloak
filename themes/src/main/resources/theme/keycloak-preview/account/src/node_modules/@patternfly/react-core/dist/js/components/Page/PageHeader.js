"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.PageHeader = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _page = _interopRequireDefault(require("@patternfly/react-styles/css/components/Page/page"));

var _reactStyles = require("@patternfly/react-styles");

var _barsIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/bars-icon"));

var _Button = require("../../components/Button");

var _Page = require("./Page");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var PageHeader = function PageHeader(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$logo = _ref.logo,
      logo = _ref$logo === void 0 ? null : _ref$logo,
      _ref$logoProps = _ref.logoProps,
      logoProps = _ref$logoProps === void 0 ? null : _ref$logoProps,
      _ref$logoComponent = _ref.logoComponent,
      logoComponent = _ref$logoComponent === void 0 ? 'a' : _ref$logoComponent,
      _ref$toolbar = _ref.toolbar,
      toolbar = _ref$toolbar === void 0 ? null : _ref$toolbar,
      _ref$avatar = _ref.avatar,
      avatar = _ref$avatar === void 0 ? null : _ref$avatar,
      _ref$topNav = _ref.topNav,
      topNav = _ref$topNav === void 0 ? null : _ref$topNav,
      _ref$isNavOpen = _ref.isNavOpen,
      isNavOpen = _ref$isNavOpen === void 0 ? true : _ref$isNavOpen,
      _ref$role = _ref.role,
      role = _ref$role === void 0 ? undefined : _ref$role,
      _ref$showNavToggle = _ref.showNavToggle,
      showNavToggle = _ref$showNavToggle === void 0 ? false : _ref$showNavToggle,
      _ref$onNavToggle = _ref.onNavToggle,
      onNavToggle = _ref$onNavToggle === void 0 ? function () {
    return undefined;
  } : _ref$onNavToggle,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Global navigation' : _ref$ariaLabel,
      props = _objectWithoutProperties(_ref, ["className", "logo", "logoProps", "logoComponent", "toolbar", "avatar", "topNav", "isNavOpen", "role", "showNavToggle", "onNavToggle", "aria-label"]);

  var LogoComponent = logoComponent;
  return React.createElement(_Page.PageContextConsumer, null, function (_ref2) {
    var isManagedSidebar = _ref2.isManagedSidebar,
        managedOnNavToggle = _ref2.onNavToggle,
        managedIsNavOpen = _ref2.isNavOpen;
    var navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
    var navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
    return React.createElement("header", _extends({
      role: role,
      className: (0, _reactStyles.css)(_page["default"].pageHeader, className)
    }, props), (showNavToggle || logo) && React.createElement("div", {
      className: (0, _reactStyles.css)(_page["default"].pageHeaderBrand)
    }, showNavToggle && React.createElement("div", {
      className: (0, _reactStyles.css)(_page["default"].pageHeaderBrandToggle)
    }, React.createElement(_Button.Button, {
      id: "nav-toggle",
      onClick: navToggle,
      "aria-label": ariaLabel,
      "aria-controls": "page-sidebar",
      "aria-expanded": navOpen ? 'true' : 'false',
      variant: _Button.ButtonVariant.plain
    }, React.createElement(_barsIcon["default"], null))), logo && React.createElement(LogoComponent, _extends({
      className: (0, _reactStyles.css)(_page["default"].pageHeaderBrandLink)
    }, logoProps), logo)), topNav && React.createElement("div", {
      className: (0, _reactStyles.css)(_page["default"].pageHeaderNav)
    }, topNav), (toolbar || avatar) && React.createElement("div", {
      className: (0, _reactStyles.css)(_page["default"].pageHeaderTools)
    }, toolbar, avatar));
  });
};

exports.PageHeader = PageHeader;
PageHeader.propTypes = {
  className: _propTypes["default"].string,
  logo: _propTypes["default"].node,
  logoProps: _propTypes["default"].object,
  logoComponent: _propTypes["default"].node,
  toolbar: _propTypes["default"].node,
  avatar: _propTypes["default"].node,
  topNav: _propTypes["default"].node,
  showNavToggle: _propTypes["default"].bool,
  isNavOpen: _propTypes["default"].bool,
  isManagedSidebar: _propTypes["default"].bool,
  role: _propTypes["default"].string,
  onNavToggle: _propTypes["default"].func,
  'aria-label': _propTypes["default"].string
};
//# sourceMappingURL=PageHeader.js.map