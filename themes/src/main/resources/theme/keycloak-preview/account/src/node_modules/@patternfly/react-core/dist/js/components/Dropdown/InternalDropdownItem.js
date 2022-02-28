"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.InternalDropdownItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _dropdownConstants = require("./dropdownConstants");

var _constants = require("../../helpers/constants");

var _Tooltip = require("../Tooltip");

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

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

var InternalDropdownItem =
/*#__PURE__*/
function (_React$Component) {
  _inherits(InternalDropdownItem, _React$Component);

  function InternalDropdownItem() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, InternalDropdownItem);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(InternalDropdownItem)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "ref", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "additionalRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "getInnerNode", function (node) {
      return node && node.childNodes && node.childNodes.length ? node.childNodes[0] : node;
    });

    _defineProperty(_assertThisInitialized(_this), "onKeyDown", function (event) {
      // Detected key press on this item, notify the menu parent so that the appropriate item can be focused
      var innerIndex = event.target === _this.ref.current ? 0 : 1;

      if (!_this.props.customChild) {
        event.preventDefault();
      }

      if (event.key === 'ArrowUp') {
        _this.props.context.keyHandler(_this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.UP);
      } else if (event.key === 'ArrowDown') {
        _this.props.context.keyHandler(_this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.DOWN);
      } else if (event.key === 'ArrowRight') {
        _this.props.context.keyHandler(_this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.RIGHT);
      } else if (event.key === 'ArrowLeft') {
        _this.props.context.keyHandler(_this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.LEFT);
      } else if (event.key === 'Enter' || event.key === ' ') {
        event.target.click();
        _this.props.enterTriggersArrowDown && _this.props.context.keyHandler(_this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.DOWN);
      }
    });

    return _this;
  }

  _createClass(InternalDropdownItem, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var _this$props = this.props,
          context = _this$props.context,
          index = _this$props.index,
          isDisabled = _this$props.isDisabled,
          role = _this$props.role,
          customChild = _this$props.customChild;
      var customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
      context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate() {
      var _this$props2 = this.props,
          context = _this$props2.context,
          index = _this$props2.index,
          isDisabled = _this$props2.isDisabled,
          role = _this$props2.role,
          customChild = _this$props2.customChild;
      var customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
      context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
    }
  }, {
    key: "extendAdditionalChildRef",
    value: function extendAdditionalChildRef() {
      var additionalChild = this.props.additionalChild;
      return React.cloneElement(additionalChild, {
        ref: this.additionalRef
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      /* eslint-disable @typescript-eslint/no-unused-vars */
      var _this$props3 = this.props,
          className = _this$props3.className,
          children = _this$props3.children,
          isHovered = _this$props3.isHovered,
          context = _this$props3.context,
          _onClick = _this$props3.onClick,
          component = _this$props3.component,
          variant = _this$props3.variant,
          role = _this$props3.role,
          isDisabled = _this$props3.isDisabled,
          index = _this$props3.index,
          href = _this$props3.href,
          tooltip = _this$props3.tooltip,
          tooltipProps = _this$props3.tooltipProps,
          id = _this$props3.id,
          componentID = _this$props3.componentID,
          listItemClassName = _this$props3.listItemClassName,
          additionalChild = _this$props3.additionalChild,
          customChild = _this$props3.customChild,
          enterTriggersArrowDown = _this$props3.enterTriggersArrowDown,
          additionalProps = _objectWithoutProperties(_this$props3, ["className", "children", "isHovered", "context", "onClick", "component", "variant", "role", "isDisabled", "index", "href", "tooltip", "tooltipProps", "id", "componentID", "listItemClassName", "additionalChild", "customChild", "enterTriggersArrowDown"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      var Component = component;
      var classes;

      if (Component === 'a') {
        additionalProps['aria-disabled'] = isDisabled;
        additionalProps.tabIndex = isDisabled ? -1 : additionalProps.tabIndex;
      } else if (Component === 'button') {
        additionalProps.disabled = isDisabled;
        additionalProps.type = additionalProps.type || 'button';
      }

      var renderWithTooltip = function renderWithTooltip(childNode) {
        return tooltip ? React.createElement(_Tooltip.Tooltip, _extends({
          content: tooltip
        }, tooltipProps), childNode) : childNode;
      };

      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref) {
        var onSelect = _ref.onSelect,
            itemClass = _ref.itemClass,
            disabledClass = _ref.disabledClass,
            hoverClass = _ref.hoverClass;

        if (_this2.props.role === 'separator') {
          classes = (0, _reactStyles.css)(variant === 'icon' && _dropdown["default"].modifiers.icon, className);
        } else {
          classes = (0, _reactStyles.css)(variant === 'icon' && _dropdown["default"].modifiers.icon, className, isDisabled && disabledClass, isHovered && hoverClass, itemClass);
        }

        if (customChild) {
          return React.cloneElement(customChild, {
            ref: _this2.ref,
            onKeyDown: _this2.onKeyDown
          });
        }

        return React.createElement("li", {
          className: listItemClassName || null,
          role: role,
          onKeyDown: _this2.onKeyDown,
          onClick: function onClick(event) {
            if (!isDisabled) {
              _onClick(event);

              onSelect(event);
            }
          },
          id: id
        }, renderWithTooltip(React.isValidElement(component) ? React.cloneElement(component, _objectSpread({
          href: href,
          id: componentID,
          className: classes
        }, additionalProps)) : React.createElement(Component, _extends({}, additionalProps, {
          href: href,
          ref: _this2.ref,
          className: classes,
          id: componentID
        }), children)), additionalChild && _this2.extendAdditionalChildRef());
      });
    }
  }]);

  return InternalDropdownItem;
}(React.Component);

exports.InternalDropdownItem = InternalDropdownItem;

_defineProperty(InternalDropdownItem, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  listItemClassName: _propTypes["default"].string,
  component: _propTypes["default"].node,
  variant: _propTypes["default"].oneOf(['item', 'icon']),
  role: _propTypes["default"].string,
  isDisabled: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  href: _propTypes["default"].string,
  tooltip: _propTypes["default"].node,
  tooltipProps: _propTypes["default"].any,
  index: _propTypes["default"].number,
  context: _propTypes["default"].shape({
    keyHandler: _propTypes["default"].func,
    sendRef: _propTypes["default"].func
  }),
  onClick: _propTypes["default"].func,
  id: _propTypes["default"].string,
  componentID: _propTypes["default"].string,
  additionalChild: _propTypes["default"].node,
  customChild: _propTypes["default"].node,
  enterTriggersArrowDown: _propTypes["default"].bool
});

_defineProperty(InternalDropdownItem, "defaultProps", {
  className: '',
  isHovered: false,
  component: 'a',
  variant: 'item',
  role: 'none',
  isDisabled: false,
  tooltipProps: {},
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: function onClick(event) {
    return undefined;
  },
  index: -1,
  context: {
    keyHandler: function keyHandler() {},
    sendRef: function sendRef() {}
  },
  enterTriggersArrowDown: false
});
//# sourceMappingURL=InternalDropdownItem.js.map