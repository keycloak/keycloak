import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
import _typeof from "@babel/runtime/helpers/typeof";
// Published as 'yaml/pair'
import addComment from '../addComment';
import { Type } from '../constants';
import toJSON from '../toJSON';
import Collection from './Collection';
import Node from './Node';
import Scalar from './Scalar';

var stringifyKey = function stringifyKey(key, jsKey, ctx) {
  if (jsKey === null) return '';
  if (_typeof(jsKey) !== 'object') return String(jsKey);
  if (key instanceof Node && ctx && ctx.doc) return key.toString({
    anchors: {},
    doc: ctx.doc,
    indent: '',
    inFlow: true,
    inStringifyKey: true
  });
  return JSON.stringify(jsKey);
};

var Pair = /*#__PURE__*/function (_Node) {
  _inherits(Pair, _Node);

  function Pair(key) {
    var _this;

    var value = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;

    _classCallCheck(this, Pair);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Pair).call(this));
    _this.key = key;
    _this.value = value;
    _this.type = 'PAIR';
    return _this;
  }

  _createClass(Pair, [{
    key: "addToJSMap",
    value: function addToJSMap(ctx, map) {
      var key = toJSON(this.key, '', ctx);

      if (map instanceof Map) {
        var value = toJSON(this.value, key, ctx);
        map.set(key, value);
      } else if (map instanceof Set) {
        map.add(key);
      } else {
        var stringKey = stringifyKey(this.key, key, ctx);
        map[stringKey] = toJSON(this.value, stringKey, ctx);
      }

      return map;
    }
  }, {
    key: "toJSON",
    value: function toJSON(_, ctx) {
      var pair = ctx && ctx.mapAsMap ? new Map() : {};
      return this.addToJSMap(ctx, pair);
    }
  }, {
    key: "toString",
    value: function toString(ctx, onComment, onChompKeep) {
      if (!ctx || !ctx.doc) return JSON.stringify(this);
      var simpleKeys = ctx.doc.options.simpleKeys;
      var key = this.key,
          value = this.value;
      var keyComment = key instanceof Node && key.comment;

      if (simpleKeys) {
        if (keyComment) {
          throw new Error('With simple keys, key nodes cannot have comments');
        }

        if (key instanceof Collection) {
          var msg = 'With simple keys, collection cannot be used as a key value';
          throw new Error(msg);
        }
      }

      var explicitKey = !simpleKeys && (!key || keyComment || key instanceof Collection || key.type === Type.BLOCK_FOLDED || key.type === Type.BLOCK_LITERAL);
      var _ctx = ctx,
          doc = _ctx.doc,
          indent = _ctx.indent;
      ctx = Object.assign({}, ctx, {
        implicitKey: !explicitKey,
        indent: indent + '  '
      });
      var chompKeep = false;
      var str = doc.schema.stringify(key, ctx, function () {
        return keyComment = null;
      }, function () {
        return chompKeep = true;
      });
      str = addComment(str, ctx.indent, keyComment);

      if (ctx.allNullValues && !simpleKeys) {
        if (this.comment) {
          str = addComment(str, ctx.indent, this.comment);
          if (onComment) onComment();
        } else if (chompKeep && !keyComment && onChompKeep) onChompKeep();

        return ctx.inFlow ? str : "? ".concat(str);
      }

      str = explicitKey ? "? ".concat(str, "\n").concat(indent, ":") : "".concat(str, ":");

      if (this.comment) {
        // expected (but not strictly required) to be a single-line comment
        str = addComment(str, ctx.indent, this.comment);
        if (onComment) onComment();
      }

      var vcb = '';
      var valueComment = null;

      if (value instanceof Node) {
        if (value.spaceBefore) vcb = '\n';

        if (value.commentBefore) {
          var cs = value.commentBefore.replace(/^/gm, "".concat(ctx.indent, "#"));
          vcb += "\n".concat(cs);
        }

        valueComment = value.comment;
      } else if (value && _typeof(value) === 'object') {
        value = doc.schema.createNode(value, true);
      }

      ctx.implicitKey = false;
      if (!explicitKey && !this.comment && value instanceof Scalar) ctx.indentAtStart = str.length + 1;
      chompKeep = false;
      var valueStr = doc.schema.stringify(value, ctx, function () {
        return valueComment = null;
      }, function () {
        return chompKeep = true;
      });
      var ws = ' ';

      if (vcb || this.comment) {
        ws = "".concat(vcb, "\n").concat(ctx.indent);
      } else if (!explicitKey && value instanceof Collection) {
        var flow = valueStr[0] === '[' || valueStr[0] === '{';
        if (!flow || valueStr.includes('\n')) ws = "\n".concat(ctx.indent);
      }

      if (chompKeep && !valueComment && onChompKeep) onChompKeep();
      return addComment(str + ws + valueStr, ctx.indent, valueComment);
    }
  }, {
    key: "commentBefore",
    get: function get() {
      return this.key && this.key.commentBefore;
    },
    set: function set(cb) {
      if (this.key == null) this.key = new Scalar(null);
      this.key.commentBefore = cb;
    }
  }]);

  return Pair;
}(Node);

export { Pair as default };