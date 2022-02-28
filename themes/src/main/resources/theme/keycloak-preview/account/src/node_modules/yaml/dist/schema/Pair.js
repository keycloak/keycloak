"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _addComment = _interopRequireDefault(require("../addComment"));

var _constants = require("../constants");

var _toJSON = _interopRequireDefault(require("../toJSON"));

var _Collection = _interopRequireDefault(require("./Collection"));

var _Node = _interopRequireDefault(require("./Node"));

var _Scalar = _interopRequireDefault(require("./Scalar"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// Published as 'yaml/pair'
const stringifyKey = (key, jsKey, ctx) => {
  if (jsKey === null) return '';
  if (typeof jsKey !== 'object') return String(jsKey);
  if (key instanceof _Node.default && ctx && ctx.doc) return key.toString({
    anchors: {},
    doc: ctx.doc,
    indent: '',
    inFlow: true,
    inStringifyKey: true
  });
  return JSON.stringify(jsKey);
};

class Pair extends _Node.default {
  constructor(key, value = null) {
    super();
    this.key = key;
    this.value = value;
    this.type = 'PAIR';
  }

  get commentBefore() {
    return this.key && this.key.commentBefore;
  }

  set commentBefore(cb) {
    if (this.key == null) this.key = new _Scalar.default(null);
    this.key.commentBefore = cb;
  }

  addToJSMap(ctx, map) {
    const key = (0, _toJSON.default)(this.key, '', ctx);

    if (map instanceof Map) {
      const value = (0, _toJSON.default)(this.value, key, ctx);
      map.set(key, value);
    } else if (map instanceof Set) {
      map.add(key);
    } else {
      const stringKey = stringifyKey(this.key, key, ctx);
      map[stringKey] = (0, _toJSON.default)(this.value, stringKey, ctx);
    }

    return map;
  }

  toJSON(_, ctx) {
    const pair = ctx && ctx.mapAsMap ? new Map() : {};
    return this.addToJSMap(ctx, pair);
  }

  toString(ctx, onComment, onChompKeep) {
    if (!ctx || !ctx.doc) return JSON.stringify(this);
    const {
      simpleKeys
    } = ctx.doc.options;
    let {
      key,
      value
    } = this;
    let keyComment = key instanceof _Node.default && key.comment;

    if (simpleKeys) {
      if (keyComment) {
        throw new Error('With simple keys, key nodes cannot have comments');
      }

      if (key instanceof _Collection.default) {
        const msg = 'With simple keys, collection cannot be used as a key value';
        throw new Error(msg);
      }
    }

    const explicitKey = !simpleKeys && (!key || keyComment || key instanceof _Collection.default || key.type === _constants.Type.BLOCK_FOLDED || key.type === _constants.Type.BLOCK_LITERAL);
    const {
      doc,
      indent
    } = ctx;
    ctx = Object.assign({}, ctx, {
      implicitKey: !explicitKey,
      indent: indent + '  '
    });
    let chompKeep = false;
    let str = doc.schema.stringify(key, ctx, () => keyComment = null, () => chompKeep = true);
    str = (0, _addComment.default)(str, ctx.indent, keyComment);

    if (ctx.allNullValues && !simpleKeys) {
      if (this.comment) {
        str = (0, _addComment.default)(str, ctx.indent, this.comment);
        if (onComment) onComment();
      } else if (chompKeep && !keyComment && onChompKeep) onChompKeep();

      return ctx.inFlow ? str : `? ${str}`;
    }

    str = explicitKey ? `? ${str}\n${indent}:` : `${str}:`;

    if (this.comment) {
      // expected (but not strictly required) to be a single-line comment
      str = (0, _addComment.default)(str, ctx.indent, this.comment);
      if (onComment) onComment();
    }

    let vcb = '';
    let valueComment = null;

    if (value instanceof _Node.default) {
      if (value.spaceBefore) vcb = '\n';

      if (value.commentBefore) {
        const cs = value.commentBefore.replace(/^/gm, `${ctx.indent}#`);
        vcb += `\n${cs}`;
      }

      valueComment = value.comment;
    } else if (value && typeof value === 'object') {
      value = doc.schema.createNode(value, true);
    }

    ctx.implicitKey = false;
    if (!explicitKey && !this.comment && value instanceof _Scalar.default) ctx.indentAtStart = str.length + 1;
    chompKeep = false;
    const valueStr = doc.schema.stringify(value, ctx, () => valueComment = null, () => chompKeep = true);
    let ws = ' ';

    if (vcb || this.comment) {
      ws = `${vcb}\n${ctx.indent}`;
    } else if (!explicitKey && value instanceof _Collection.default) {
      const flow = valueStr[0] === '[' || valueStr[0] === '{';
      if (!flow || valueStr.includes('\n')) ws = `\n${ctx.indent}`;
    }

    if (chompKeep && !valueComment && onChompKeep) onChompKeep();
    return (0, _addComment.default)(str + ws + valueStr, ctx.indent, valueComment);
  }

}

exports.default = Pair;