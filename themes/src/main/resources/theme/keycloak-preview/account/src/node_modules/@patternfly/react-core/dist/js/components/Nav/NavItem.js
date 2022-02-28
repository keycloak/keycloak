"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.NavItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _nav = _interopRequireDefault(require("@patternfly/react-styles/css/components/Nav/nav"));

var _reactStyles = require("@patternfly/react-styles");

var _Nav = require("./Nav");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var NavItem = function NavItem(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$to = _ref.to,
      to = _ref$to === void 0 ? '' : _ref$to,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$groupId = _ref.groupId,
      groupId = _ref$groupId === void 0 ? null : _ref$groupId,
      _ref$itemId = _ref.itemId,
      itemId = _ref$itemId === void 0 ? null : _ref$itemId,
      _ref$preventDefault = _ref.preventDefault,
      preventDefault = _ref$preventDefault === void 0 ? false : _ref$preventDefault,
      _ref$onClick = _ref.onClick,
      _onClick = _ref$onClick === void 0 ? null : _ref$onClick,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? 'a' : _ref$component,
      props = _objectWithoutProperties(_ref, ["children", "className", "to", "isActive", "groupId", "itemId", "preventDefault", "onClick", "component"]);

  var Component = component;

  var renderDefaultLink = function renderDefaultLink() {
    var preventLinkDefault = preventDefault || !to;
    return React.createElement(_Nav.NavContext.Consumer, null, function (context) {
      return React.createElement(Component, _extends({
        href: to,
        onClick: function onClick(e) {
          return context.onSelect(e, groupId, itemId, to, preventLinkDefault, _onClick);
        },
        className: (0, _reactStyles.css)(_nav["default"].navLink, isActive && _nav["default"].modifiers.current, className),
        "aria-current": isActive ? 'page' : null
      }, props), children);
    });
  };

  var renderClonedChild = function renderClonedChild(child) {
    return React.createElement(_Nav.NavContext.Consumer, null, function (context) {
      return React.cloneElement(child, {
        onClick: function onClick(e) {
          return context.onSelect(e, groupId, itemId, to, preventDefault, _onClick);
        },
        className: (0, _reactStyles.css)(_nav["default"].navLink, isActive && _nav["default"].modifiers.current, child.props && child.props.className),
        'aria-current': isActive ? 'page' : null
      });
    });
  };

  return React.createElement("li", {
    className: (0, _reactStyles.css)(_nav["default"].navItem, className)
  }, React.isValidElement(children) ? renderClonedChild(children) : renderDefaultLink());
};

exports.NavItem = NavItem;
NavItem.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  to: _propTypes["default"].string,
  isActive: _propTypes["default"].bool,
  groupId: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].number, _propTypes["default"].oneOf([null])]),
  itemId: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].number, _propTypes["default"].oneOf([null])]),
  preventDefault: _propTypes["default"].bool,
  onClick: _propTypes["default"].func,
  component: _propTypes["default"].node
};
//# sourceMappingURL=NavItem.js.map