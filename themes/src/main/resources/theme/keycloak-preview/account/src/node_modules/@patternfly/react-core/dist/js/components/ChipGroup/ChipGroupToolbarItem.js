"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ChipGroupToolbarItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _chipGroup = _interopRequireDefault(require("@patternfly/react-styles/css/components/ChipGroup/chip-group"));

var _ChipGroup = require("./ChipGroup");

var _ChipButton = require("./ChipButton");

var _Tooltip = require("../Tooltip");

var _timesIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/times-icon"));

var _GenerateId = _interopRequireDefault(require("../../helpers/GenerateId/GenerateId"));

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

var ChipGroupToolbarItem =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ChipGroupToolbarItem, _React$Component);

  function ChipGroupToolbarItem(props) {
    var _this;

    _classCallCheck(this, ChipGroupToolbarItem);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(ChipGroupToolbarItem).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "heading", React.createRef());

    _this.state = {
      isTooltipVisible: false
    };
    return _this;
  }

  _createClass(ChipGroupToolbarItem, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      this.setState({
        isTooltipVisible: Boolean(this.heading.current && this.heading.current.offsetWidth < this.heading.current.scrollWidth)
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          categoryName = _this$props.categoryName,
          children = _this$props.children,
          className = _this$props.className,
          isClosable = _this$props.isClosable,
          closeBtnAriaLabel = _this$props.closeBtnAriaLabel,
          onClick = _this$props.onClick,
          tooltipPosition = _this$props.tooltipPosition,
          rest = _objectWithoutProperties(_this$props, ["categoryName", "children", "className", "isClosable", "closeBtnAriaLabel", "onClick", "tooltipPosition"]);

      if (React.Children.count(children)) {
        var renderChipGroup = function renderChipGroup(id, HeadingLevel) {
          return React.createElement("ul", _extends({
            className: (0, _reactStyles.css)(_chipGroup["default"].chipGroup, _chipGroup["default"].modifiers.toolbar, className)
          }, rest), React.createElement("li", null, _this2.state.isTooltipVisible ? React.createElement(_Tooltip.Tooltip, {
            position: tooltipPosition,
            content: categoryName
          }, React.createElement(HeadingLevel, {
            tabIndex: "0",
            ref: _this2.heading,
            className: (0, _reactStyles.css)(_chipGroup["default"].chipGroupLabel),
            id: id
          }, categoryName)) : React.createElement(HeadingLevel, {
            ref: _this2.heading,
            className: (0, _reactStyles.css)(_chipGroup["default"].chipGroupLabel),
            id: id
          }, categoryName), React.createElement("ul", {
            className: (0, _reactStyles.css)(_chipGroup["default"].chipGroup)
          }, children), isClosable && React.createElement("div", {
            className: "pf-c-chip-group__close"
          }, React.createElement(_ChipButton.ChipButton, {
            "aria-label": closeBtnAriaLabel,
            onClick: onClick,
            id: "remove_group_".concat(id),
            "aria-labelledby": "remove_group_".concat(id, " ").concat(id)
          }, React.createElement(_timesIcon["default"], {
            "aria-hidden": "true"
          })))));
        };

        return React.createElement(_ChipGroup.ChipGroupContext.Consumer, null, function (HeadingLevel) {
          return React.createElement(_GenerateId["default"], null, function (randomId) {
            return renderChipGroup(randomId, HeadingLevel);
          });
        });
      }

      return null;
    }
  }]);

  return ChipGroupToolbarItem;
}(React.Component);

exports.ChipGroupToolbarItem = ChipGroupToolbarItem;

_defineProperty(ChipGroupToolbarItem, "propTypes", {
  categoryName: _propTypes["default"].string,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isClosable: _propTypes["default"].bool,
  onClick: _propTypes["default"].func,
  closeBtnAriaLabel: _propTypes["default"].string,
  tooltipPosition: _propTypes["default"].oneOf(['auto', 'top', 'bottom', 'left', 'right'])
});

_defineProperty(ChipGroupToolbarItem, "defaultProps", {
  categoryName: '',
  children: null,
  className: '',
  isClosable: false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: function onClick(_e) {
    return undefined;
  },
  closeBtnAriaLabel: 'Close chip group',
  tooltipPosition: 'top'
});
//# sourceMappingURL=ChipGroupToolbarItem.js.map