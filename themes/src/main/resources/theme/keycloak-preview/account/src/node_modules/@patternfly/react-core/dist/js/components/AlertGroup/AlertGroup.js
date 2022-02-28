"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AlertGroup = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _helpers = require("../../helpers");

var _AlertGroupInline = require("./AlertGroupInline");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var AlertGroup =
/*#__PURE__*/
function (_React$Component) {
  _inherits(AlertGroup, _React$Component);

  function AlertGroup() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, AlertGroup);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(AlertGroup)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "state", {
      container: undefined
    });

    return _this;
  }

  _createClass(AlertGroup, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var container = document.createElement('div');
      var target = this.getTargetElement();
      this.setState({
        container: container
      });
      target.appendChild(container);
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      var target = this.getTargetElement();

      if (this.state.container) {
        target.removeChild(this.state.container);
      }
    }
  }, {
    key: "getTargetElement",
    value: function getTargetElement() {
      var appendTo = this.props.appendTo;

      if (typeof appendTo === 'function') {
        return appendTo();
      }

      return appendTo || document.body;
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props = this.props,
          className = _this$props.className,
          children = _this$props.children,
          isToast = _this$props.isToast;
      var alertGroup = React.createElement(_AlertGroupInline.AlertGroupInline, {
        className: className,
        isToast: isToast
      }, children);

      if (!this.props.isToast) {
        return alertGroup;
      }

      var container = this.state.container;

      if (!_helpers.canUseDOM || !container) {
        return null;
      }

      return ReactDOM.createPortal(alertGroup, container);
    }
  }]);

  return AlertGroup;
}(React.Component);

exports.AlertGroup = AlertGroup;

_defineProperty(AlertGroup, "propTypes", {
  className: _propTypes["default"].string,
  children: _propTypes["default"].node,
  isToast: _propTypes["default"].bool,
  appendTo: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].func])
});
//# sourceMappingURL=AlertGroup.js.map