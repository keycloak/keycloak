"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataToolbarToggleGroup = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _dataToolbar = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"));

var _reactStyles = require("@patternfly/react-styles");

var _DataToolbarUtils = require("./DataToolbarUtils");

var _Button = require("../../components/Button");

var _global_breakpoint_lg = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_lg"));

var _util = require("../../helpers/util");

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

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DataToolbarToggleGroup =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DataToolbarToggleGroup, _React$Component);

  function DataToolbarToggleGroup() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, DataToolbarToggleGroup);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(DataToolbarToggleGroup)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "isContentPopup", function () {
      var viewportSize = window.innerWidth;
      var lgBreakpointValue = parseInt(_global_breakpoint_lg["default"].value);
      return viewportSize < lgBreakpointValue;
    });

    return _this;
  }

  _createClass(DataToolbarToggleGroup, [{
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          toggleIcon = _this$props.toggleIcon,
          breakpoint = _this$props.breakpoint,
          variant = _this$props.variant,
          breakpointMods = _this$props.breakpointMods,
          className = _this$props.className,
          children = _this$props.children,
          props = _objectWithoutProperties(_this$props, ["toggleIcon", "breakpoint", "variant", "breakpointMods", "className", "children"]);

      return React.createElement(_DataToolbarUtils.DataToolbarContext.Consumer, null, function (_ref) {
        var isExpanded = _ref.isExpanded,
            toggleIsExpanded = _ref.toggleIsExpanded;
        return React.createElement(_DataToolbarUtils.DataToolbarContentContext.Consumer, null, function (_ref2) {
          var expandableContentRef = _ref2.expandableContentRef,
              expandableContentId = _ref2.expandableContentId;

          if (expandableContentRef.current && expandableContentRef.current.classList) {
            if (isExpanded) {
              expandableContentRef.current.classList.add((0, _reactStyles.getModifier)(_dataToolbar["default"], 'expanded'));
            } else {
              expandableContentRef.current.classList.remove((0, _reactStyles.getModifier)(_dataToolbar["default"], 'expanded'));
            }
          }

          return React.createElement("div", _extends({
            className: (0, _reactStyles.css)(_dataToolbar["default"].dataToolbarGroup, variant && (0, _reactStyles.getModifier)(_dataToolbar["default"], variant), (0, _util.formatBreakpointMods)(breakpointMods, _dataToolbar["default"]), (0, _reactStyles.getModifier)(_dataToolbar["default"], 'toggle-group'), (0, _reactStyles.getModifier)(_dataToolbar["default"], "show-on-".concat(breakpoint)), className)
          }, props), React.createElement("div", {
            className: (0, _reactStyles.css)(_dataToolbar["default"].dataToolbarToggle)
          }, React.createElement(_Button.Button, _extends({
            variant: "plain",
            onClick: toggleIsExpanded,
            "aria-label": "Show Filters"
          }, isExpanded && {
            'aria-expanded': true
          }, {
            "aria-haspopup": isExpanded && _this2.isContentPopup(),
            "aria-controls": expandableContentId
          }), toggleIcon)), isExpanded ? ReactDOM.createPortal(children, expandableContentRef.current.firstElementChild) : children);
        });
      });
    }
  }]);

  return DataToolbarToggleGroup;
}(React.Component);

exports.DataToolbarToggleGroup = DataToolbarToggleGroup;

_defineProperty(DataToolbarToggleGroup, "propTypes", {
  toggleIcon: _propTypes["default"].node.isRequired,
  breakpoint: _propTypes["default"].oneOf(['md', 'lg', 'xl']).isRequired,
  breakpointMods: _propTypes["default"].arrayOf(_propTypes["default"].any)
});

_defineProperty(DataToolbarToggleGroup, "defaultProps", {
  breakpointMods: []
});
//# sourceMappingURL=DataToolbarToggleGroup.js.map