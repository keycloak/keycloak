"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Dropdown = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

var _dropdownConstants = require("./dropdownConstants");

var _DropdownWithContext = require("./DropdownWithContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var Dropdown = function Dropdown(_ref) {
  var _onSelect = _ref.onSelect,
      ref = _ref.ref,
      props = _objectWithoutProperties(_ref, ["onSelect", "ref"]);

  return React.createElement(_dropdownConstants.DropdownContext.Provider, {
    value: {
      onSelect: function onSelect(event) {
        return _onSelect && _onSelect(event);
      },
      toggleTextClass: _dropdown["default"].dropdownToggleText,
      toggleIconClass: _dropdown["default"].dropdownToggleIcon,
      menuClass: _dropdown["default"].dropdownMenu,
      itemClass: _dropdown["default"].dropdownMenuItem,
      toggleClass: _dropdown["default"].dropdownToggle,
      baseClass: _dropdown["default"].dropdown,
      baseComponent: 'div',
      sectionClass: _dropdown["default"].dropdownGroup,
      sectionTitleClass: _dropdown["default"].dropdownGroupTitle,
      sectionComponent: 'section',
      disabledClass: _dropdown["default"].modifiers.disabled,
      hoverClass: _dropdown["default"].modifiers.hover,
      separatorClass: _dropdown["default"].dropdownSeparator
    }
  }, React.createElement(_DropdownWithContext.DropdownWithContext, props));
};

exports.Dropdown = Dropdown;
Dropdown.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  dropdownItems: _propTypes["default"].arrayOf(_propTypes["default"].any),
  isOpen: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  position: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['right']), _propTypes["default"].oneOf(['left'])]),
  direction: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['up']), _propTypes["default"].oneOf(['down'])]),
  isGrouped: _propTypes["default"].bool,
  toggle: _propTypes["default"].element.isRequired,
  onSelect: _propTypes["default"].func,
  autoFocus: _propTypes["default"].bool,
  ouiaComponentType: _propTypes["default"].string
};
//# sourceMappingURL=Dropdown.js.map