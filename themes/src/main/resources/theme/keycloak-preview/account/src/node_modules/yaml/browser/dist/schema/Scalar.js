import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
// Published as 'yaml/scalar'
import _toJSON from '../toJSON';
import Node from './Node';

var Scalar = /*#__PURE__*/function (_Node) {
  _inherits(Scalar, _Node);

  function Scalar(value) {
    var _this;

    _classCallCheck(this, Scalar);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Scalar).call(this));
    _this.value = value;
    return _this;
  }

  _createClass(Scalar, [{
    key: "toJSON",
    value: function toJSON(arg, ctx) {
      return ctx && ctx.keep ? this.value : _toJSON(this.value, arg, ctx);
    }
  }, {
    key: "toString",
    value: function toString() {
      return String(this.value);
    }
  }]);

  return Scalar;
}(Node);

export { Scalar as default };