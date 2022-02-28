"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ClipboardCopy = exports.ClipboardCopyVariant = exports.clipboardCopyFunc = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _clipboardCopy = _interopRequireDefault(require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"));

var _reactStyles = require("@patternfly/react-styles");

var _TextInput = require("../TextInput");

var _Tooltip = require("../Tooltip");

var _GenerateId = _interopRequireDefault(require("../../helpers/GenerateId/GenerateId"));

var _ClipboardCopyButton = require("./ClipboardCopyButton");

var _ClipboardCopyToggle = require("./ClipboardCopyToggle");

var _ClipboardCopyExpanded = require("./ClipboardCopyExpanded");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var clipboardCopyFunc = function clipboardCopyFunc(event, text) {
  var clipboard = event.currentTarget.parentElement;
  var el = document.createElement('input');
  el.value = text.toString();
  clipboard.appendChild(el);
  el.select();
  document.execCommand('copy');
  clipboard.removeChild(el);
};

exports.clipboardCopyFunc = clipboardCopyFunc;
var ClipboardCopyVariant;
exports.ClipboardCopyVariant = ClipboardCopyVariant;

(function (ClipboardCopyVariant) {
  ClipboardCopyVariant["inline"] = "inline";
  ClipboardCopyVariant["expansion"] = "expansion";
})(ClipboardCopyVariant || (exports.ClipboardCopyVariant = ClipboardCopyVariant = {}));

var ClipboardCopy =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ClipboardCopy, _React$Component);

  function ClipboardCopy(props) {
    var _this;

    _classCallCheck(this, ClipboardCopy);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(ClipboardCopy).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "timer", null);

    _defineProperty(_assertThisInitialized(_this), "componentDidUpdate", function (prevProps, prevState) {
      if (prevProps.children !== _this.props.children) {
        _this.updateText(_this.props.children);
      }
    });

    _defineProperty(_assertThisInitialized(_this), "expandContent", function (_event) {
      _this.setState(function (prevState) {
        return {
          expanded: !prevState.expanded
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "updateText", function (text) {
      _this.setState({
        text: text
      });

      _this.props.onChange(text);
    });

    _defineProperty(_assertThisInitialized(_this), "render", function () {
      var _this$props = _this.props,
          isExpanded = _this$props.isExpanded,
          onChange = _this$props.onChange,
          isReadOnly = _this$props.isReadOnly,
          isCode = _this$props.isCode,
          exitDelay = _this$props.exitDelay,
          maxWidth = _this$props.maxWidth,
          entryDelay = _this$props.entryDelay,
          switchDelay = _this$props.switchDelay,
          onCopy = _this$props.onCopy,
          hoverTip = _this$props.hoverTip,
          clickTip = _this$props.clickTip,
          textAriaLabel = _this$props.textAriaLabel,
          toggleAriaLabel = _this$props.toggleAriaLabel,
          variant = _this$props.variant,
          position = _this$props.position,
          className = _this$props.className,
          divProps = _objectWithoutProperties(_this$props, ["isExpanded", "onChange", "isReadOnly", "isCode", "exitDelay", "maxWidth", "entryDelay", "switchDelay", "onCopy", "hoverTip", "clickTip", "textAriaLabel", "toggleAriaLabel", "variant", "position", "className"]);

      var textIdPrefix = 'text-input-';
      var toggleIdPrefix = 'toggle-';
      var contentIdPrefix = 'content-';
      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_clipboardCopy["default"].clipboardCopy, _this.state.expanded && _clipboardCopy["default"].modifiers.expanded, className)
      }, divProps), React.createElement(_GenerateId["default"], {
        prefix: ""
      }, function (id) {
        return React.createElement(React.Fragment, null, React.createElement("div", {
          className: (0, _reactStyles.css)(_clipboardCopy["default"].clipboardCopyGroup)
        }, variant === 'expansion' && React.createElement(_ClipboardCopyToggle.ClipboardCopyToggle, {
          isExpanded: _this.state.expanded,
          onClick: _this.expandContent,
          id: "".concat(toggleIdPrefix, "-").concat(id),
          textId: "".concat(textIdPrefix, "-").concat(id),
          contentId: "".concat(contentIdPrefix, "-").concat(id),
          "aria-label": toggleAriaLabel
        }), React.createElement(_TextInput.TextInput, {
          isReadOnly: isReadOnly || _this.state.expanded,
          onChange: _this.updateText,
          value: _this.state.text,
          id: "text-input-".concat(id),
          "aria-label": textAriaLabel
        }), React.createElement(_ClipboardCopyButton.ClipboardCopyButton, {
          exitDelay: exitDelay,
          entryDelay: entryDelay,
          maxWidth: maxWidth,
          position: position,
          id: "copy-button-".concat(id),
          textId: "text-input-".concat(id),
          "aria-label": hoverTip,
          onClick: function onClick(event) {
            if (_this.timer) {
              window.clearTimeout(_this.timer);

              _this.setState({
                copied: false
              });
            }

            onCopy(event, _this.state.text);

            _this.setState({
              copied: true
            }, function () {
              _this.timer = window.setTimeout(function () {
                _this.setState({
                  copied: false
                });

                _this.timer = null;
              }, switchDelay);
            });
          }
        }, _this.state.copied ? clickTip : hoverTip)), _this.state.expanded && React.createElement(_ClipboardCopyExpanded.ClipboardCopyExpanded, {
          isReadOnly: isReadOnly,
          isCode: isCode,
          id: "content-".concat(id),
          onChange: _this.updateText
        }, _this.state.text));
      }));
    });

    _this.state = {
      text: _this.props.children,
      expanded: _this.props.isExpanded,
      copied: false
    };
    return _this;
  }

  return ClipboardCopy;
}(React.Component);

exports.ClipboardCopy = ClipboardCopy;

_defineProperty(ClipboardCopy, "propTypes", {
  className: _propTypes["default"].string,
  hoverTip: _propTypes["default"].string,
  clickTip: _propTypes["default"].string,
  textAriaLabel: _propTypes["default"].string,
  toggleAriaLabel: _propTypes["default"].string,
  isReadOnly: _propTypes["default"].bool,
  isExpanded: _propTypes["default"].bool,
  isCode: _propTypes["default"].bool,
  variant: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['inline']), _propTypes["default"].oneOf(['expansion'])]),
  position: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['auto']), _propTypes["default"].oneOf(['top']), _propTypes["default"].oneOf(['bottom']), _propTypes["default"].oneOf(['left']), _propTypes["default"].oneOf(['right'])]),
  maxWidth: _propTypes["default"].string,
  exitDelay: _propTypes["default"].number,
  entryDelay: _propTypes["default"].number,
  switchDelay: _propTypes["default"].number,
  onCopy: _propTypes["default"].func,
  onChange: _propTypes["default"].func,
  children: _propTypes["default"].node.isRequired
});

_defineProperty(ClipboardCopy, "defaultProps", {
  hoverTip: 'Copy to clipboard',
  clickTip: 'Successfully copied to clipboard!',
  isReadOnly: false,
  isExpanded: false,
  isCode: false,
  variant: 'inline',
  position: _Tooltip.TooltipPosition.top,
  maxWidth: '150px',
  exitDelay: 1600,
  entryDelay: 100,
  switchDelay: 2000,
  onCopy: clipboardCopyFunc,
  onChange: function onChange() {
    return undefined;
  },
  textAriaLabel: 'Copyable input',
  toggleAriaLabel: 'Show content'
});
//# sourceMappingURL=ClipboardCopy.js.map