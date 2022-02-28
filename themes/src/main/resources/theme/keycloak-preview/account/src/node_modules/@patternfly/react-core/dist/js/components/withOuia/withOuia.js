"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.withOuiaContext = withOuiaContext;
exports.OuiaContext = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _ouia = require("./ouia");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var OuiaContext = React.createContext(null);
exports.OuiaContext = OuiaContext;

/**
 * @param { React.ComponentClass | React.FunctionComponent } WrappedComponent - React component
 */
function withOuiaContext(WrappedComponent) {
  /* eslint-disable react/display-name */
  return function (props) {
    return React.createElement(OuiaContext.Consumer, null, function (value) {
      return React.createElement(ComponentWithOuia, {
        consumerContext: value,
        component: WrappedComponent,
        componentProps: props
      });
    });
  };
  /* eslint-enable react/display-name */
}

var ComponentWithOuia =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ComponentWithOuia, _React$Component);

  function ComponentWithOuia(props) {
    var _this;

    _classCallCheck(this, ComponentWithOuia);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(ComponentWithOuia).call(this, props));
    _this.state = {
      isOuia: false,
      ouiaId: null
    };
    return _this;
  }
  /**
   * if either consumer set isOuia through context or local storage
   * then force a re-render
   */


  _createClass(ComponentWithOuia, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var _this$state = this.state,
          isOuia = _this$state.isOuia,
          ouiaId = _this$state.ouiaId;
      var consumerContext = this.props.consumerContext;
      var isOuiaEnv = (0, _ouia.isOUIAEnvironment)();

      if (consumerContext && consumerContext.isOuia !== undefined && consumerContext.isOuia !== isOuia || isOuiaEnv !== isOuia) {
        this.setState({
          isOuia: consumerContext && consumerContext.isOuia !== undefined ? consumerContext.isOuia : isOuiaEnv,
          ouiaId: consumerContext && consumerContext.ouiaId !== undefined ? consumerContext.ouiaId : (0, _ouia.generateOUIAId)() ? (0, _ouia.getUniqueId)() : ouiaId
        });
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this$state2 = this.state,
          isOuia = _this$state2.isOuia,
          ouiaId = _this$state2.ouiaId;
      var _this$props = this.props,
          WrappedComponent = _this$props.component,
          componentProps = _this$props.componentProps,
          consumerContext = _this$props.consumerContext;
      return React.createElement(OuiaContext.Provider, {
        value: {
          isOuia: consumerContext && consumerContext.isOuia || isOuia,
          ouiaId: consumerContext && consumerContext.ouiaId || ouiaId
        }
      }, React.createElement(OuiaContext.Consumer, null, function (value) {
        return React.createElement(WrappedComponent, _extends({}, componentProps, {
          ouiaContext: value
        }));
      }));
    }
  }]);

  return ComponentWithOuia;
}(React.Component);

_defineProperty(ComponentWithOuia, "propTypes", {
  component: _propTypes["default"].any.isRequired,
  componentProps: _propTypes["default"].any.isRequired,
  consumerContext: _propTypes["default"].shape({
    isOuia: _propTypes["default"].bool,
    ouiaId: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string])
  })
});
//# sourceMappingURL=withOuia.js.map