"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Modal = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _helpers = require("../../helpers");

var _reactStyles = require("@patternfly/react-styles");

var _backdrop = _interopRequireDefault(require("@patternfly/react-styles/css/components/Backdrop/backdrop"));

var _constants = require("../../helpers/constants");

var _ModalContent = require("./ModalContent");

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

var Modal =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Modal, _React$Component);

  function Modal(props) {
    var _this;

    _classCallCheck(this, Modal);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Modal).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "id", '');

    _defineProperty(_assertThisInitialized(_this), "handleEscKeyClick", function (event) {
      if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && _this.props.isOpen) {
        _this.props.onClose();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "getElement", function (appendTo) {
      var target;

      if (typeof appendTo === 'function') {
        target = appendTo();
      } else {
        target = appendTo;
      }

      return target;
    });

    _defineProperty(_assertThisInitialized(_this), "toggleSiblingsFromScreenReaders", function (hide) {
      var appendTo = _this.props.appendTo;

      var target = _this.getElement(appendTo);

      var bodyChildren = target.children;

      for (var _i = 0, _Array$from = Array.from(bodyChildren); _i < _Array$from.length; _i++) {
        var child = _Array$from[_i];

        if (child !== _this.state.container) {
          hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
        }
      }
    });

    var newId = Modal.currentId++;
    _this.id = "pf-modal-".concat(newId);
    _this.state = {
      container: undefined
    };
    return _this;
  }

  _createClass(Modal, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var appendTo = this.props.appendTo;
      var target = this.getElement(appendTo);
      var container = document.createElement('div');
      this.setState({
        container: container
      });
      target.appendChild(container);
      target.addEventListener('keydown', this.handleEscKeyClick, false);

      if (this.props.isOpen) {
        target.classList.add((0, _reactStyles.css)(_backdrop["default"].backdropOpen));
      } else {
        target.classList.remove((0, _reactStyles.css)(_backdrop["default"].backdropOpen));
      }
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate() {
      var appendTo = this.props.appendTo;
      var target = this.getElement(appendTo);

      if (this.props.isOpen) {
        target.classList.add((0, _reactStyles.css)(_backdrop["default"].backdropOpen));
        this.toggleSiblingsFromScreenReaders(true);
      } else {
        target.classList.remove((0, _reactStyles.css)(_backdrop["default"].backdropOpen));
        this.toggleSiblingsFromScreenReaders(false);
      }
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      var appendTo = this.props.appendTo;
      var target = this.getElement(appendTo);

      if (this.state.container) {
        target.removeChild(this.state.container);
      }

      target.removeEventListener('keydown', this.handleEscKeyClick, false);
      target.classList.remove((0, _reactStyles.css)(_backdrop["default"].backdropOpen));
    }
  }, {
    key: "render",
    value: function render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props = this.props,
          appendTo = _this$props.appendTo,
          props = _objectWithoutProperties(_this$props, ["appendTo"]);

      var container = this.state.container;

      if (!_helpers.canUseDOM || !container) {
        return null;
      }

      return ReactDOM.createPortal(React.createElement(_ModalContent.ModalContent, _extends({}, props, {
        title: this.props.title,
        id: this.id,
        ariaDescribedById: this.props.ariaDescribedById
      })), container);
    }
  }]);

  return Modal;
}(React.Component);

exports.Modal = Modal;

_defineProperty(Modal, "propTypes", {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  header: _propTypes["default"].node,
  title: _propTypes["default"].string.isRequired,
  hideTitle: _propTypes["default"].bool,
  showClose: _propTypes["default"].bool,
  ariaDescribedById: _propTypes["default"].string,
  footer: _propTypes["default"].node,
  actions: _propTypes["default"].any,
  isFooterLeftAligned: _propTypes["default"].bool,
  onClose: _propTypes["default"].func,
  width: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string]),
  isLarge: _propTypes["default"].bool,
  isSmall: _propTypes["default"].bool,
  appendTo: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].func]),
  disableFocusTrap: _propTypes["default"].bool,
  description: _propTypes["default"].node
});

_defineProperty(Modal, "currentId", 0);

_defineProperty(Modal, "defaultProps", {
  className: '',
  isOpen: false,
  hideTitle: false,
  showClose: true,
  ariaDescribedById: '',
  actions: [],
  isFooterLeftAligned: false,
  onClose: function onClose() {
    return undefined;
  },
  isLarge: false,
  isSmall: false,
  appendTo: typeof document !== 'undefined' && document.body || null
});
//# sourceMappingURL=Modal.js.map