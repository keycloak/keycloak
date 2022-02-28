"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownWithContext = void 0;

var React = _interopRequireWildcard(require("react"));

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

var _reactStyles = require("@patternfly/react-styles");

var _DropdownMenu = require("./DropdownMenu");

var _dropdownConstants = require("./dropdownConstants");

var _withOuia = require("../withOuia");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

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

var DropdownWithContext =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DropdownWithContext, _React$Component);

  // seed for the aria-labelledby ID
  function DropdownWithContext(props) {
    var _this;

    _classCallCheck(this, DropdownWithContext);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(DropdownWithContext).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "openedOnEnter", false);

    _defineProperty(_assertThisInitialized(_this), "baseComponentRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "onEnter", function () {
      _this.openedOnEnter = true;
    });

    if (props.dropdownItems && props.dropdownItems.length > 0 && props.children) {
      // eslint-disable-next-line no-console
      console.error('Children and dropdownItems props have been provided. Only the dropdownItems prop items will be rendered');
    }

    return _this;
  }

  _createClass(DropdownWithContext, [{
    key: "componentDidUpdate",
    value: function componentDidUpdate() {
      if (!this.props.isOpen) {
        this.openedOnEnter = false;
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          children = _this$props.children,
          className = _this$props.className,
          direction = _this$props.direction,
          dropdownItems = _this$props.dropdownItems,
          isOpen = _this$props.isOpen,
          isPlain = _this$props.isPlain,
          isGrouped = _this$props.isGrouped,
          onSelect = _this$props.onSelect,
          position = _this$props.position,
          toggle = _this$props.toggle,
          autoFocus = _this$props.autoFocus,
          ouiaContext = _this$props.ouiaContext,
          ouiaId = _this$props.ouiaId,
          ouiaComponentType = _this$props.ouiaComponentType,
          props = _objectWithoutProperties(_this$props, ["children", "className", "direction", "dropdownItems", "isOpen", "isPlain", "isGrouped", "onSelect", "position", "toggle", "autoFocus", "ouiaContext", "ouiaId", "ouiaComponentType"]);

      var id = toggle.props.id || "pf-toggle-id-".concat(DropdownWithContext.currentId++);
      var component;
      var renderedContent;
      var ariaHasPopup = false;

      if (dropdownItems && dropdownItems.length > 0) {
        component = 'ul';
        renderedContent = dropdownItems;
        ariaHasPopup = true;
      } else {
        component = 'div';
        renderedContent = React.Children.toArray(children);
      }

      var openedOnEnter = this.openedOnEnter;
      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref) {
        var baseClass = _ref.baseClass,
            baseComponent = _ref.baseComponent,
            contextId = _ref.id;
        var BaseComponent = baseComponent;
        return React.createElement(BaseComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(baseClass, direction === _dropdownConstants.DropdownDirection.up && _dropdown["default"].modifiers.top, position === _dropdownConstants.DropdownPosition.right && _dropdown["default"].modifiers.alignRight, isOpen && _dropdown["default"].modifiers.expanded, className),
          ref: _this2.baseComponentRef
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': ouiaComponentType,
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.Children.map(toggle, function (oneToggle) {
          return React.cloneElement(oneToggle, {
            parentRef: _this2.baseComponentRef,
            isOpen: isOpen,
            id: id,
            isPlain: isPlain,
            ariaHasPopup: ariaHasPopup,
            onEnter: function onEnter() {
              return _this2.onEnter();
            }
          });
        }), isOpen && React.createElement(_DropdownMenu.DropdownMenu, {
          component: component,
          isOpen: isOpen,
          position: position,
          "aria-labelledby": contextId ? "".concat(contextId, "-toggle") : id,
          openedOnEnter: openedOnEnter,
          isGrouped: isGrouped,
          autoFocus: openedOnEnter && autoFocus
        }, renderedContent));
      });
    }
  }]);

  return DropdownWithContext;
}(React.Component);

_defineProperty(DropdownWithContext, "currentId", 0);

_defineProperty(DropdownWithContext, "defaultProps", {
  className: '',
  dropdownItems: [],
  isOpen: false,
  isPlain: false,
  isGrouped: false,
  position: _dropdownConstants.DropdownPosition.left,
  direction: _dropdownConstants.DropdownDirection.down,
  onSelect: function onSelect() {
    return undefined;
  },
  autoFocus: true,
  ouiaComponentType: 'Dropdown'
});

var DropdownWithOuiaContext = (0, _withOuia.withOuiaContext)(DropdownWithContext);
exports.DropdownWithContext = DropdownWithOuiaContext;
//# sourceMappingURL=DropdownWithContext.js.map