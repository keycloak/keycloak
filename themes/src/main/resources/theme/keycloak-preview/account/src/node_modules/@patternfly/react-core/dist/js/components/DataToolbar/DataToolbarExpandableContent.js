"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataToolbarExpandableContent = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dataToolbar = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"));

var _reactStyles = require("@patternfly/react-styles");

var _DataToolbarGroup = require("./DataToolbarGroup");

var _DataToolbarItem = require("./DataToolbarItem");

var _Button = require("../../components/Button");

var _DataToolbarUtils = require("./DataToolbarUtils");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DataToolbarExpandableContent =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DataToolbarExpandableContent, _React$Component);

  function DataToolbarExpandableContent() {
    _classCallCheck(this, DataToolbarExpandableContent);

    return _possibleConstructorReturn(this, _getPrototypeOf(DataToolbarExpandableContent).apply(this, arguments));
  }

  _createClass(DataToolbarExpandableContent, [{
    key: "render",
    value: function render() {
      var _this$props = this.props,
          className = _this$props.className,
          expandableContentRef = _this$props.expandableContentRef,
          chipContainerRef = _this$props.chipContainerRef,
          isExpanded = _this$props.isExpanded,
          clearAllFilters = _this$props.clearAllFilters,
          clearFiltersButtonText = _this$props.clearFiltersButtonText,
          showClearFiltersButton = _this$props.showClearFiltersButton,
          props = _objectWithoutProperties(_this$props, ["className", "expandableContentRef", "chipContainerRef", "isExpanded", "clearAllFilters", "clearFiltersButtonText", "showClearFiltersButton"]);

      var numberOfFilters = this.context.numberOfFilters;

      var clearChipGroups = function clearChipGroups() {
        clearAllFilters();
      };

      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_dataToolbar["default"].dataToolbarExpandableContent, className),
        ref: expandableContentRef
      }, props), React.createElement(_DataToolbarGroup.DataToolbarGroup, null), numberOfFilters > 0 && React.createElement(_DataToolbarGroup.DataToolbarGroup, {
        className: (0, _reactStyles.getModifier)(_dataToolbar["default"], 'chip-container')
      }, React.createElement(_DataToolbarGroup.DataToolbarGroup, {
        ref: chipContainerRef
      }), showClearFiltersButton && React.createElement(_DataToolbarItem.DataToolbarItem, {
        className: (0, _reactStyles.css)((0, _reactStyles.getModifier)(_dataToolbar["default"], 'clear'))
      }, React.createElement(_Button.Button, {
        variant: "link",
        onClick: clearChipGroups,
        isInline: true
      }, clearFiltersButtonText))));
    }
  }]);

  return DataToolbarExpandableContent;
}(React.Component);

exports.DataToolbarExpandableContent = DataToolbarExpandableContent;

_defineProperty(DataToolbarExpandableContent, "propTypes", {
  className: _propTypes["default"].string,
  isExpanded: _propTypes["default"].bool,
  expandableContentRef: _propTypes["default"].any,
  chipContainerRef: _propTypes["default"].any,
  clearAllFilters: _propTypes["default"].func,
  clearFiltersButtonText: _propTypes["default"].string,
  showClearFiltersButton: _propTypes["default"].bool.isRequired
});

_defineProperty(DataToolbarExpandableContent, "contextType", _DataToolbarUtils.DataToolbarContext);

_defineProperty(DataToolbarExpandableContent, "defaultProps", {
  isExpanded: false,
  clearFiltersButtonText: 'Clear all filters'
});
//# sourceMappingURL=DataToolbarExpandableContent.js.map