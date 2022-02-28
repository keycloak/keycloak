"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _toJSON = _interopRequireDefault(require("../toJSON"));

var _Node = _interopRequireDefault(require("./Node"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// Published as 'yaml/scalar'
class Scalar extends _Node.default {
  constructor(value) {
    super();
    this.value = value;
  }

  toJSON(arg, ctx) {
    return ctx && ctx.keep ? this.value : (0, _toJSON.default)(this.value, arg, ctx);
  }

  toString() {
    return String(this.value);
  }

}

exports.default = Scalar;