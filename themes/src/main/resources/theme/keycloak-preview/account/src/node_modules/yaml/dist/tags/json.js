"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _map = _interopRequireDefault(require("./failsafe/map"));

var _seq = _interopRequireDefault(require("./failsafe/seq"));

var _Scalar = _interopRequireDefault(require("../schema/Scalar"));

var _string = require("./failsafe/string");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const schema = [_map.default, _seq.default, {
  identify: value => typeof value === 'string',
  default: true,
  tag: 'tag:yaml.org,2002:str',
  resolve: _string.resolveString,
  stringify: value => JSON.stringify(value)
}, {
  identify: value => value == null,
  createNode: (schema, value, ctx) => ctx.wrapScalars ? new _Scalar.default(null) : null,
  default: true,
  tag: 'tag:yaml.org,2002:null',
  test: /^null$/,
  resolve: () => null,
  stringify: value => JSON.stringify(value)
}, {
  identify: value => typeof value === 'boolean',
  default: true,
  tag: 'tag:yaml.org,2002:bool',
  test: /^true|false$/,
  resolve: str => str === 'true',
  stringify: value => JSON.stringify(value)
}, {
  identify: value => typeof value === 'number',
  default: true,
  tag: 'tag:yaml.org,2002:int',
  test: /^-?(?:0|[1-9][0-9]*)$/,
  resolve: str => parseInt(str, 10),
  stringify: value => JSON.stringify(value)
}, {
  identify: value => typeof value === 'number',
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^-?(?:0|[1-9][0-9]*)(?:\.[0-9]*)?(?:[eE][-+]?[0-9]+)?$/,
  resolve: str => parseFloat(str),
  stringify: value => JSON.stringify(value)
}];

schema.scalarFallback = str => {
  throw new SyntaxError(`Unresolved plain scalar ${JSON.stringify(str)}`);
};

var _default = schema;
exports.default = _default;