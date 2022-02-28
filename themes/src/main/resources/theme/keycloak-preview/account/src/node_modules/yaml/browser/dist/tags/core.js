import Scalar from '../schema/Scalar';
import { stringifyNumber } from '../stringify';
import failsafe from './failsafe';
import { boolOptions, nullOptions } from './options';
export var nullObj = {
  identify: function identify(value) {
    return value == null;
  },
  createNode: function createNode(schema, value, ctx) {
    return ctx.wrapScalars ? new Scalar(null) : null;
  },
  default: true,
  tag: 'tag:yaml.org,2002:null',
  test: /^(?:~|[Nn]ull|NULL)?$/,
  resolve: function resolve() {
    return null;
  },
  options: nullOptions,
  stringify: function stringify() {
    return nullOptions.nullStr;
  }
};
export var boolObj = {
  identify: function identify(value) {
    return typeof value === 'boolean';
  },
  default: true,
  tag: 'tag:yaml.org,2002:bool',
  test: /^(?:[Tt]rue|TRUE|[Ff]alse|FALSE)$/,
  resolve: function resolve(str) {
    return str[0] === 't' || str[0] === 'T';
  },
  options: boolOptions,
  stringify: function stringify(_ref) {
    var value = _ref.value;
    return value ? boolOptions.trueStr : boolOptions.falseStr;
  }
};
export var octObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'OCT',
  test: /^0o([0-7]+)$/,
  resolve: function resolve(str, oct) {
    return parseInt(oct, 8);
  },
  stringify: function stringify(_ref2) {
    var value = _ref2.value;
    return '0o' + value.toString(8);
  }
};
export var intObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  test: /^[-+]?[0-9]+$/,
  resolve: function resolve(str) {
    return parseInt(str, 10);
  },
  stringify: stringifyNumber
};
export var hexObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'HEX',
  test: /^0x([0-9a-fA-F]+)$/,
  resolve: function resolve(str, hex) {
    return parseInt(hex, 16);
  },
  stringify: function stringify(_ref3) {
    var value = _ref3.value;
    return '0x' + value.toString(16);
  }
};
export var nanObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^(?:[-+]?\.inf|(\.nan))$/i,
  resolve: function resolve(str, nan) {
    return nan ? NaN : str[0] === '-' ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
  },
  stringify: stringifyNumber
};
export var expObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  format: 'EXP',
  test: /^[-+]?(?:0|[1-9][0-9]*)(\.[0-9]*)?[eE][-+]?[0-9]+$/,
  resolve: function resolve(str) {
    return parseFloat(str);
  },
  stringify: function stringify(_ref4) {
    var value = _ref4.value;
    return Number(value).toExponential();
  }
};
export var floatObj = {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^[-+]?(?:0|[1-9][0-9]*)\.([0-9]*)$/,
  resolve: function resolve(str, frac) {
    var node = new Scalar(parseFloat(str));
    if (frac && frac[frac.length - 1] === '0') node.minFractionDigits = frac.length;
    return node;
  },
  stringify: stringifyNumber
};
export default failsafe.concat([nullObj, boolObj, octObj, intObj, hexObj, nanObj, expObj, floatObj]);