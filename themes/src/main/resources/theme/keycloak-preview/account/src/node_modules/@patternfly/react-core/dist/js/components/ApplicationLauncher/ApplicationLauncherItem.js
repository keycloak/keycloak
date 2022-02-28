"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ApplicationLauncherItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _appLauncher = _interopRequireDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));

var _Dropdown = require("../Dropdown");

var _ApplicationLauncherContent = require("./ApplicationLauncherContent");

var _ApplicationLauncherContext = require("./ApplicationLauncherContext");

var _ApplicationLauncherItemContext = require("./ApplicationLauncherItemContext");

var _starIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/star-icon"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var ApplicationLauncherItem = function ApplicationLauncherItem(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      id = _ref.id,
      children = _ref.children,
      _ref$icon = _ref.icon,
      icon = _ref$icon === void 0 ? null : _ref$icon,
      _ref$isExternal = _ref.isExternal,
      isExternal = _ref$isExternal === void 0 ? false : _ref$isExternal,
      href = _ref.href,
      _ref$tooltip = _ref.tooltip,
      tooltip = _ref$tooltip === void 0 ? null : _ref$tooltip,
      _ref$tooltipProps = _ref.tooltipProps,
      tooltipProps = _ref$tooltipProps === void 0 ? null : _ref$tooltipProps,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? 'a' : _ref$component,
      _ref$isFavorite = _ref.isFavorite,
      isFavorite = _ref$isFavorite === void 0 ? null : _ref$isFavorite,
      _ref$ariaIsFavoriteLa = _ref.ariaIsFavoriteLabel,
      ariaIsFavoriteLabel = _ref$ariaIsFavoriteLa === void 0 ? 'starred' : _ref$ariaIsFavoriteLa,
      _ref$ariaIsNotFavorit = _ref.ariaIsNotFavoriteLabel,
      ariaIsNotFavoriteLabel = _ref$ariaIsNotFavorit === void 0 ? 'not starred' : _ref$ariaIsNotFavorit,
      customChild = _ref.customChild,
      _ref$enterTriggersArr = _ref.enterTriggersArrowDown,
      enterTriggersArrowDown = _ref$enterTriggersArr === void 0 ? false : _ref$enterTriggersArr,
      props = _objectWithoutProperties(_ref, ["className", "id", "children", "icon", "isExternal", "href", "tooltip", "tooltipProps", "component", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "customChild", "enterTriggersArrowDown"]);

  return React.createElement(_ApplicationLauncherItemContext.ApplicationLauncherItemContext.Provider, {
    value: {
      isExternal: isExternal,
      icon: icon
    }
  }, React.createElement(_ApplicationLauncherContext.ApplicationLauncherContext.Consumer, null, function (_ref2) {
    var onFavorite = _ref2.onFavorite;
    return React.createElement(_Dropdown.DropdownItem, _extends({
      id: id,
      component: component,
      href: href || null,
      className: (0, _reactStyles.css)(isExternal && _appLauncher["default"].modifiers.external, isFavorite !== null && _appLauncher["default"].modifiers.link, className),
      listItemClassName: (0, _reactStyles.css)(onFavorite && _appLauncher["default"].appLauncherMenuWrapper, isFavorite && _appLauncher["default"].modifiers.favorite),
      tooltip: tooltip,
      tooltipProps: tooltipProps
    }, enterTriggersArrowDown === true && {
      enterTriggersArrowDown: enterTriggersArrowDown
    }, customChild && {
      customChild: customChild
    }, isFavorite !== null && {
      additionalChild: React.createElement("button", {
        className: (0, _reactStyles.css)(_appLauncher["default"].appLauncherMenuItem, _appLauncher["default"].modifiers.action),
        "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel,
        onClick: function onClick() {
          onFavorite(id, isFavorite);
        }
      }, React.createElement(_starIcon["default"], null))
    }, props), children && React.createElement(_ApplicationLauncherContent.ApplicationLauncherContent, null, children));
  }));
};

exports.ApplicationLauncherItem = ApplicationLauncherItem;
ApplicationLauncherItem.propTypes = {
  icon: _propTypes["default"].node,
  isExternal: _propTypes["default"].bool,
  tooltip: _propTypes["default"].node,
  tooltipProps: _propTypes["default"].any,
  component: _propTypes["default"].node,
  isFavorite: _propTypes["default"].bool,
  ariaIsFavoriteLabel: _propTypes["default"].string,
  ariaIsNotFavoriteLabel: _propTypes["default"].string,
  id: _propTypes["default"].string,
  customChild: _propTypes["default"].node,
  enterTriggersArrowDown: _propTypes["default"].bool
};
//# sourceMappingURL=ApplicationLauncherItem.js.map