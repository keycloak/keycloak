import map from './failsafe/map';
import seq from './failsafe/seq';
import Scalar from '../schema/Scalar';
import { resolveString } from './failsafe/string';
var schema = [map, seq, {
  identify: function identify(value) {
    return typeof value === 'string';
  },
  default: true,
  tag: 'tag:yaml.org,2002:str',
  resolve: resolveString,
  stringify: function stringify(value) {
    return JSON.stringify(value);
  }
}, {
  identify: function identify(value) {
    return value == null;
  },
  createNode: function createNode(schema, value, ctx) {
    return ctx.wrapScalars ? new Scalar(null) : null;
  },
  default: true,
  tag: 'tag:yaml.org,2002:null',
  test: /^null$/,
  resolve: function resolve() {
    return null;
  },
  stringify: function stringify(value) {
    return JSON.stringify(value);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'boolean';
  },
  default: true,
  tag: 'tag:yaml.org,2002:bool',
  test: /^true|false$/,
  resolve: function resolve(str) {
    return str === 'true';
  },
  stringify: function stringify(value) {
    return JSON.stringify(value);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  test: /^-?(?:0|[1-9][0-9]*)$/,
  resolve: function resolve(str) {
    return parseInt(str, 10);
  },
  stringify: function stringify(value) {
    return JSON.stringify(value);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^-?(?:0|[1-9][0-9]*)(?:\.[0-9]*)?(?:[eE][-+]?[0-9]+)?$/,
  resolve: function resolve(str) {
    return parseFloat(str);
  },
  stringify: function stringify(value) {
    return JSON.stringify(value);
  }
}];

schema.scalarFallback = function (str) {
  throw new SyntaxError("Unresolved plain scalar ".concat(JSON.stringify(str)));
};

export default schema;