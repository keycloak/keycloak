"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.SelectToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _select = _interopRequireDefault(require("@patternfly/react-styles/css/components/Select/select"));

var _button = _interopRequireDefault(require("@patternfly/react-styles/css/components/Button/button"));

var _reactStyles = require("@patternfly/react-styles");

var _caretDownIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/caret-down-icon"));

var _selectConstants = require("./selectConstants");

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

var SelectToggle =
/*#__PURE__*/
function (_React$Component) {
  _inherits(SelectToggle, _React$Component);

  function SelectToggle(props) {
    var _this;

    _classCallCheck(this, SelectToggle);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(SelectToggle).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "onDocClick", function (event) {
      var _this$props = _this.props,
          parentRef = _this$props.parentRef,
          isExpanded = _this$props.isExpanded,
          onToggle = _this$props.onToggle,
          onClose = _this$props.onClose;

      if (isExpanded && parentRef && !parentRef.current.contains(event.target)) {
        onToggle(false);
        onClose();

        _this.toggle.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onEscPress", function (event) {
      var _this$props2 = _this.props,
          parentRef = _this$props2.parentRef,
          isExpanded = _this$props2.isExpanded,
          variant = _this$props2.variant,
          onToggle = _this$props2.onToggle,
          onClose = _this$props2.onClose;

      if (event.key === _selectConstants.KeyTypes.Tab && variant === _selectConstants.SelectVariant.checkbox) {
        return;
      }

      if (isExpanded && (event.key === _selectConstants.KeyTypes.Escape || event.key === _selectConstants.KeyTypes.Tab) && parentRef && parentRef.current.contains(event.target)) {
        onToggle(false);
        onClose();

        _this.toggle.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onKeyDown", function (event) {
      var _this$props3 = _this.props,
          isExpanded = _this$props3.isExpanded,
          onToggle = _this$props3.onToggle,
          variant = _this$props3.variant,
          onClose = _this$props3.onClose,
          onEnter = _this$props3.onEnter,
          handleTypeaheadKeys = _this$props3.handleTypeaheadKeys;

      if ((event.key === _selectConstants.KeyTypes.ArrowDown || event.key === _selectConstants.KeyTypes.ArrowUp) && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
        handleTypeaheadKeys(event.key === _selectConstants.KeyTypes.ArrowDown && 'down' || event.key === _selectConstants.KeyTypes.ArrowUp && 'up');
      }

      if (event.key === _selectConstants.KeyTypes.Enter && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
        if (isExpanded) {
          handleTypeaheadKeys('enter');
        } else {
          onToggle(!isExpanded);
        }
      }

      if (event.key === _selectConstants.KeyTypes.Tab && variant === _selectConstants.SelectVariant.checkbox || event.key === _selectConstants.KeyTypes.Tab && !isExpanded || event.key !== _selectConstants.KeyTypes.Enter && event.key !== _selectConstants.KeyTypes.Space || (event.key === _selectConstants.KeyTypes.Space || event.key === _selectConstants.KeyTypes.Enter) && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
        return;
      }

      event.preventDefault();

      if ((event.key === _selectConstants.KeyTypes.Tab || event.key === _selectConstants.KeyTypes.Enter || event.key === _selectConstants.KeyTypes.Space) && isExpanded) {
        onToggle(!isExpanded);
        onClose();

        _this.toggle.current.focus();
      } else if ((event.key === _selectConstants.KeyTypes.Enter || event.key === _selectConstants.KeyTypes.Space) && !isExpanded) {
        onToggle(!isExpanded);
        onEnter();
      }
    });

    var _variant = props.variant;
    var isTypeahead = _variant === _selectConstants.SelectVariant.typeahead || _variant === _selectConstants.SelectVariant.typeaheadMulti;
    _this.toggle = isTypeahead ? React.createRef() : React.createRef();
    return _this;
  }

  _createClass(SelectToggle, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      document.addEventListener('mousedown', this.onDocClick);
      document.addEventListener('touchstart', this.onDocClick);
      document.addEventListener('keydown', this.onEscPress);
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      document.removeEventListener('mousedown', this.onDocClick);
      document.removeEventListener('touchstart', this.onDocClick);
      document.removeEventListener('keydown', this.onEscPress);
    }
  }, {
    key: "render",
    value: function render() {
      /* eslint-disable @typescript-eslint/no-unused-vars */
      var _this$props4 = this.props,
          className = _this$props4.className,
          children = _this$props4.children,
          isExpanded = _this$props4.isExpanded,
          isFocused = _this$props4.isFocused,
          isActive = _this$props4.isActive,
          isHovered = _this$props4.isHovered,
          isPlain = _this$props4.isPlain,
          isDisabled = _this$props4.isDisabled,
          variant = _this$props4.variant,
          onToggle = _this$props4.onToggle,
          onEnter = _this$props4.onEnter,
          onClose = _this$props4.onClose,
          handleTypeaheadKeys = _this$props4.handleTypeaheadKeys,
          parentRef = _this$props4.parentRef,
          id = _this$props4.id,
          type = _this$props4.type,
          hasClearButton = _this$props4.hasClearButton,
          ariaLabelledBy = _this$props4.ariaLabelledBy,
          ariaLabelToggle = _this$props4.ariaLabelToggle,
          props = _objectWithoutProperties(_this$props4, ["className", "children", "isExpanded", "isFocused", "isActive", "isHovered", "isPlain", "isDisabled", "variant", "onToggle", "onEnter", "onClose", "handleTypeaheadKeys", "parentRef", "id", "type", "hasClearButton", "ariaLabelledBy", "ariaLabelToggle"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      var isTypeahead = variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti || hasClearButton;
      var toggleProps = {
        id: id,
        'aria-labelledby': ariaLabelledBy,
        'aria-expanded': isExpanded,
        'aria-haspopup': variant !== _selectConstants.SelectVariant.checkbox && 'listbox' || null
      };
      return React.createElement(React.Fragment, null, !isTypeahead && React.createElement("button", _extends({}, props, toggleProps, {
        ref: this.toggle,
        type: type,
        className: (0, _reactStyles.css)(_select["default"].selectToggle, isFocused && _select["default"].modifiers.focus, isHovered && _select["default"].modifiers.hover, isDisabled && _select["default"].modifiers.disabled, isActive && _select["default"].modifiers.active, isPlain && _select["default"].modifiers.plain, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
        ,
        onClick: function onClick(_event) {
          onToggle(!isExpanded);

          if (isExpanded) {
            onClose();
          }
        },
        onKeyDown: this.onKeyDown,
        disabled: isDisabled
      }), children, React.createElement(_caretDownIcon["default"], {
        className: (0, _reactStyles.css)(_select["default"].selectToggleArrow)
      })), isTypeahead && React.createElement("div", _extends({}, props, {
        ref: this.toggle,
        className: (0, _reactStyles.css)(_select["default"].selectToggle, isFocused && _select["default"].modifiers.focus, isHovered && _select["default"].modifiers.hover, isActive && _select["default"].modifiers.active, isDisabled && _select["default"].modifiers.disabled, isPlain && _select["default"].modifiers.plain, isTypeahead && _select["default"].modifiers.typeahead, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
        ,
        onClick: function onClick(_event) {
          if (!isDisabled) {
            onToggle(true);
          }
        },
        onKeyDown: this.onKeyDown
      }), children, React.createElement("button", _extends({}, toggleProps, {
        type: type,
        className: (0, _reactStyles.css)(_button["default"].button, _select["default"].selectToggleButton, _select["default"].modifiers.plain),
        "aria-label": ariaLabelToggle,
        onClick: function onClick(_event) {
          _event.stopPropagation();

          onToggle(!isExpanded);

          if (isExpanded) {
            onClose();
          }
        },
        disabled: isDisabled
      }), React.createElement(_caretDownIcon["default"], {
        className: (0, _reactStyles.css)(_select["default"].selectToggleArrow)
      }))));
    }
  }]);

  return SelectToggle;
}(React.Component);

exports.SelectToggle = SelectToggle;

_defineProperty(SelectToggle, "propTypes", {
  id: _propTypes["default"].string.isRequired,
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isExpanded: _propTypes["default"].bool,
  onToggle: _propTypes["default"].func,
  onEnter: _propTypes["default"].func,
  onClose: _propTypes["default"].func,
  handleTypeaheadKeys: _propTypes["default"].func,
  parentRef: _propTypes["default"].any.isRequired,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  type: _propTypes["default"].oneOfType([_propTypes["default"].oneOf(['reset']), _propTypes["default"].oneOf(['button']), _propTypes["default"].oneOf(['submit'])]),
  ariaLabelledBy: _propTypes["default"].string,
  ariaLabelToggle: _propTypes["default"].string,
  variant: _propTypes["default"].oneOf(['single', 'checkbox', 'typeahead', 'typeaheadmulti']),
  hasClearButton: _propTypes["default"].bool
});

_defineProperty(SelectToggle, "defaultProps", {
  className: '',
  isExpanded: false,
  isFocused: false,
  isHovered: false,
  isActive: false,
  isPlain: false,
  isDisabled: false,
  hasClearButton: false,
  variant: 'single',
  ariaLabelledBy: '',
  ariaLabelToggle: '',
  type: 'button',
  onToggle: function onToggle() {},
  onEnter: function onEnter() {},
  onClose: function onClose() {}
});
//# sourceMappingURL=SelectToggle.js.map