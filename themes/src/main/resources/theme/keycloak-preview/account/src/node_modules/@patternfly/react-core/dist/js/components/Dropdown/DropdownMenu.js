"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownMenu = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

var _reactStyles = require("@patternfly/react-styles");

var _util = require("../../helpers/util");

var _dropdownConstants = require("./dropdownConstants");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DropdownMenu =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DropdownMenu, _React$Component);

  function DropdownMenu() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, DropdownMenu);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(DropdownMenu)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "refsCollection", []);

    _defineProperty(_assertThisInitialized(_this), "childKeyHandler", function (index, innerIndex, position) {
      var custom = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : false;
      (0, _util.keyHandler)(index, innerIndex, position, _this.refsCollection, _this.props.isGrouped ? _this.refsCollection : React.Children.toArray(_this.props.children), custom);
    });

    _defineProperty(_assertThisInitialized(_this), "sendRef", function (index, nodes, isDisabled, isSeparator) {
      _this.refsCollection[index] = [];
      nodes.map(function (node, innerIndex) {
        if (!node) {
          _this.refsCollection[index][innerIndex] = null;
        } else if (!node.getAttribute) {
          // eslint-disable-next-line react/no-find-dom-node
          _this.refsCollection[index][innerIndex] = ReactDOM.findDOMNode(node);
        } else if (isDisabled || isSeparator) {
          _this.refsCollection[index][innerIndex] = null;
        } else {
          _this.refsCollection[index][innerIndex] = node;
        }
      });
    });

    return _this;
  }

  _createClass(DropdownMenu, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var autoFocus = this.props.autoFocus;

      if (autoFocus) {
        // Focus first non-disabled element
        var focusTargetCollection = this.refsCollection.find(function (ref) {
          return ref && ref[0] && !ref[0].hasAttribute('disabled');
        });
        var focusTarget = focusTargetCollection && focusTargetCollection[0];

        if (focusTarget && focusTarget.focus) {
          focusTarget.focus();
        }
      }
    }
  }, {
    key: "shouldComponentUpdate",
    value: function shouldComponentUpdate() {
      // reset refsCollection before updating to account for child removal between mounts
      this.refsCollection = [];
      return true;
    }
  }, {
    key: "extendChildren",
    value: function extendChildren() {
      var _this$props = this.props,
          children = _this$props.children,
          isGrouped = _this$props.isGrouped;

      if (isGrouped) {
        var index = 0;
        return React.Children.map(children, function (groupedChildren) {
          var group = groupedChildren;
          return React.cloneElement(group, _objectSpread({}, group.props && group.props.children && {
            children: group.props.children.constructor === Array && React.Children.map(group.props.children, function (option) {
              return React.cloneElement(option, {
                index: index++
              });
            }) || React.cloneElement(group.props.children, {
              index: index++
            })
          }));
        });
      }

      return React.Children.map(children, function (child, index) {
        return React.cloneElement(child, {
          index: index
        });
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props2 = this.props,
          className = _this$props2.className,
          isOpen = _this$props2.isOpen,
          position = _this$props2.position,
          children = _this$props2.children,
          component = _this$props2.component,
          isGrouped = _this$props2.isGrouped,
          openedOnEnter = _this$props2.openedOnEnter,
          props = _objectWithoutProperties(_this$props2, ["className", "isOpen", "position", "children", "component", "isGrouped", "openedOnEnter"]);

      return React.createElement(_dropdownConstants.DropdownArrowContext.Provider, {
        value: {
          keyHandler: this.childKeyHandler,
          sendRef: this.sendRef
        }
      }, component === 'div' ? React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref) {
        var onSelect = _ref.onSelect,
            menuClass = _ref.menuClass;
        return React.createElement("div", {
          className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown["default"].modifiers.alignRight, className),
          hidden: !isOpen,
          onClick: function onClick(event) {
            return onSelect && onSelect(event);
          }
        }, children);
      }) : isGrouped && React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref2) {
        var menuClass = _ref2.menuClass,
            menuComponent = _ref2.menuComponent;
        var MenuComponent = menuComponent || 'div';
        return React.createElement(MenuComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown["default"].modifiers.alignRight, className),
          hidden: !isOpen,
          role: "menu"
        }), _this2.extendChildren());
      }) || React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref3) {
        var menuClass = _ref3.menuClass,
            menuComponent = _ref3.menuComponent;
        var MenuComponent = menuComponent || component;
        return React.createElement(MenuComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown["default"].modifiers.alignRight, className),
          hidden: !isOpen,
          role: "menu"
        }), _this2.extendChildren());
      }));
    }
  }]);

  return DropdownMenu;
}(React.Component);

exports.DropdownMenu = DropdownMenu;

_defineProperty(DropdownMenu, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  openedOnEnter: _propTypes["default"].bool,
  autoFocus: _propTypes["default"].bool,
  component: _propTypes["default"].node,
  position: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['right']), _propTypes["default"].oneOf(['left'])]),
  isGrouped: _propTypes["default"].bool
});

_defineProperty(DropdownMenu, "defaultProps", {
  className: '',
  isOpen: true,
  openedOnEnter: false,
  autoFocus: true,
  position: _dropdownConstants.DropdownPosition.left,
  component: 'ul',
  isGrouped: false
});
//# sourceMappingURL=DropdownMenu.js.map