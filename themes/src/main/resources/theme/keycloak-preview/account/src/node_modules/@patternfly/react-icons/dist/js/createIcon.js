"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _react = _interopRequireDefault(require("react"));

var _common = require("./common");

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

var currentId = 0;

var createIcon = function createIcon(iconDefinition) {
  var viewBox = [iconDefinition.xOffset || 0, iconDefinition.yOffset || 0, iconDefinition.width, iconDefinition.height].join(' ');
  var transform = iconDefinition.transform;

  var Icon =
  /*#__PURE__*/
  function (_React$Component) {
    _inherits(Icon, _React$Component);

    function Icon() {
      var _getPrototypeOf2;

      var _this;

      _classCallCheck(this, Icon);

      for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }

      _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Icon)).call.apply(_getPrototypeOf2, [this].concat(args)));

      _defineProperty(_assertThisInitialized(_this), "id", "icon-title-".concat(currentId++));

      return _this;
    }

    _createClass(Icon, [{
      key: "render",
      value: function render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        var _this$props = this.props,
            size = _this$props.size,
            color = _this$props.color,
            title = _this$props.title,
            noStyle = _this$props.noStyle,
            noVerticalAlign = _this$props.noVerticalAlign,
            props = _objectWithoutProperties(_this$props, ["size", "color", "title", "noStyle", "noVerticalAlign"]);

        var hasTitle = Boolean(title);
        var heightWidth = (0, _common.getSize)(size);
        var baseAlign = -0.125 * Number.parseFloat(heightWidth);
        var style = noVerticalAlign ? null : {
          verticalAlign: "".concat(baseAlign, "em")
        };
        return _react["default"].createElement("svg", _extends({
          style: style,
          fill: color,
          height: heightWidth,
          width: heightWidth,
          viewBox: viewBox,
          "aria-labelledby": hasTitle ? this.id : null,
          "aria-hidden": hasTitle ? null : true,
          role: "img"
        }, props), hasTitle && _react["default"].createElement("title", {
          id: this.id
        }, title), _react["default"].createElement("path", {
          d: iconDefinition.svgPath,
          transform: transform
        }));
      }
    }]);

    return Icon;
  }(_react["default"].Component);

  _defineProperty(Icon, "displayName", iconDefinition.name);

  _defineProperty(Icon, "propTypes", _common.propTypes);

  _defineProperty(Icon, "defaultProps", _common.defaultProps);

  return Icon;
};

var _default = createIcon;
exports["default"] = _default;
//# sourceMappingURL=createIcon.js.map