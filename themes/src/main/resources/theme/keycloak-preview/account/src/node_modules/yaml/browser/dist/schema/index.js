import _typeof from "@babel/runtime/helpers/typeof";
import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _defineProperty from "@babel/runtime/helpers/defineProperty";
import { warnOptionDeprecation } from '../warnings';
import { Type } from '../constants';
import { YAMLReferenceError, YAMLWarning } from '../errors';
import { stringifyString } from '../stringify';
import { schemas, tags } from '../tags';
import { resolveString } from '../tags/failsafe/string';
import Alias from './Alias';
import Collection from './Collection';
import Node from './Node';
import Pair from './Pair';
import Scalar from './Scalar';

var isMap = function isMap(_ref) {
  var type = _ref.type;
  return type === Type.FLOW_MAP || type === Type.MAP;
};

var isSeq = function isSeq(_ref2) {
  var type = _ref2.type;
  return type === Type.FLOW_SEQ || type === Type.SEQ;
};

var Schema = /*#__PURE__*/function () {
  function Schema(_ref3) {
    var customTags = _ref3.customTags,
        merge = _ref3.merge,
        schema = _ref3.schema,
        sortMapEntries = _ref3.sortMapEntries,
        deprecatedCustomTags = _ref3.tags;

    _classCallCheck(this, Schema);

    this.merge = !!merge;
    this.name = schema;
    this.sortMapEntries = sortMapEntries === true ? function (a, b) {
      return a.key < b.key ? -1 : a.key > b.key ? 1 : 0;
    } : sortMapEntries || null;
    this.tags = schemas[schema.replace(/\W/g, '')]; // 'yaml-1.1' -> 'yaml11'

    if (!this.tags) {
      var keys = Object.keys(schemas).map(function (key) {
        return JSON.stringify(key);
      }).join(', ');
      throw new Error("Unknown schema \"".concat(schema, "\"; use one of ").concat(keys));
    }

    if (!customTags && deprecatedCustomTags) {
      customTags = deprecatedCustomTags;
      warnOptionDeprecation('tags', 'customTags');
    }

    if (Array.isArray(customTags)) {
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = customTags[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var tag = _step.value;
          this.tags = this.tags.concat(tag);
        }
      } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion && _iterator.return != null) {
            _iterator.return();
          }
        } finally {
          if (_didIteratorError) {
            throw _iteratorError;
          }
        }
      }
    } else if (typeof customTags === 'function') {
      this.tags = customTags(this.tags.slice());
    }

    for (var i = 0; i < this.tags.length; ++i) {
      var _tag = this.tags[i];

      if (typeof _tag === 'string') {
        var tagObj = tags[_tag];

        if (!tagObj) {
          var _keys = Object.keys(tags).map(function (key) {
            return JSON.stringify(key);
          }).join(', ');

          throw new Error("Unknown custom tag \"".concat(_tag, "\"; use one of ").concat(_keys));
        }

        this.tags[i] = tagObj;
      }
    }
  }

  _createClass(Schema, [{
    key: "createNode",
    value: function createNode(value, wrapScalars, tag, ctx) {
      if (value instanceof Node) return value;
      var tagObj;

      if (tag) {
        if (tag.startsWith('!!')) tag = Schema.defaultPrefix + tag.slice(2);
        var match = this.tags.filter(function (t) {
          return t.tag === tag;
        });
        tagObj = match.find(function (t) {
          return !t.format;
        }) || match[0];
        if (!tagObj) throw new Error("Tag ".concat(tag, " not found"));
      } else {
        // TODO: deprecate/remove class check
        tagObj = this.tags.find(function (t) {
          return (t.identify && t.identify(value) || t.class && value instanceof t.class) && !t.format;
        });

        if (!tagObj) {
          if (typeof value.toJSON === 'function') value = value.toJSON();
          if (_typeof(value) !== 'object') return wrapScalars ? new Scalar(value) : value;
          tagObj = value instanceof Map ? tags.map : value[Symbol.iterator] ? tags.seq : tags.map;
        }
      }

      if (!ctx) ctx = {
        wrapScalars: wrapScalars
      };else ctx.wrapScalars = wrapScalars;

      if (ctx.onTagObj) {
        ctx.onTagObj(tagObj);
        delete ctx.onTagObj;
      }

      var obj = {};

      if (value && _typeof(value) === 'object' && ctx.prevObjects) {
        var prev = ctx.prevObjects.get(value);

        if (prev) {
          var alias = new Alias(prev); // leaves source dirty; must be cleaned by caller

          ctx.aliasNodes.push(alias);
          return alias;
        }

        obj.value = value;
        ctx.prevObjects.set(value, obj);
      }

      obj.node = tagObj.createNode ? tagObj.createNode(this, value, ctx) : wrapScalars ? new Scalar(value) : value;
      if (tag && obj.node instanceof Node) obj.node.tag = tag;
      return obj.node;
    }
  }, {
    key: "createPair",
    value: function createPair(key, value, ctx) {
      var k = this.createNode(key, ctx.wrapScalars, null, ctx);
      var v = this.createNode(value, ctx.wrapScalars, null, ctx);
      return new Pair(k, v);
    } // falls back to string on no match

  }, {
    key: "resolveScalar",
    value: function resolveScalar(str, tags) {
      if (!tags) tags = this.tags;

      for (var i = 0; i < tags.length; ++i) {
        var _tags$i = tags[i],
            format = _tags$i.format,
            test = _tags$i.test,
            resolve = _tags$i.resolve;

        if (test) {
          var match = str.match(test);

          if (match) {
            var res = resolve.apply(null, match);
            if (!(res instanceof Scalar)) res = new Scalar(res);
            if (format) res.format = format;
            return res;
          }
        }
      }

      if (this.tags.scalarFallback) str = this.tags.scalarFallback(str);
      return new Scalar(str);
    } // sets node.resolved on success

  }, {
    key: "resolveNode",
    value: function resolveNode(doc, node, tagName) {
      var tags = this.tags.filter(function (_ref4) {
        var tag = _ref4.tag;
        return tag === tagName;
      });
      var generic = tags.find(function (_ref5) {
        var test = _ref5.test;
        return !test;
      });
      if (node.error) doc.errors.push(node.error);

      try {
        if (generic) {
          var res = generic.resolve(doc, node);
          if (!(res instanceof Collection)) res = new Scalar(res);
          node.resolved = res;
        } else {
          var str = resolveString(doc, node);

          if (typeof str === 'string' && tags.length > 0) {
            node.resolved = this.resolveScalar(str, tags);
          }
        }
      } catch (error) {
        /* istanbul ignore if */
        if (!error.source) error.source = node;
        doc.errors.push(error);
        node.resolved = null;
      }

      if (!node.resolved) return null;
      if (tagName && node.tag) node.resolved.tag = tagName;
      return node.resolved;
    }
  }, {
    key: "resolveNodeWithFallback",
    value: function resolveNodeWithFallback(doc, node, tagName) {
      var res = this.resolveNode(doc, node, tagName);
      if (Object.prototype.hasOwnProperty.call(node, 'resolved')) return res;
      var fallback = isMap(node) ? Schema.defaultTags.MAP : isSeq(node) ? Schema.defaultTags.SEQ : Schema.defaultTags.STR;
      /* istanbul ignore else */

      if (fallback) {
        doc.warnings.push(new YAMLWarning(node, "The tag ".concat(tagName, " is unavailable, falling back to ").concat(fallback)));

        var _res = this.resolveNode(doc, node, fallback);

        _res.tag = tagName;
        return _res;
      } else {
        doc.errors.push(new YAMLReferenceError(node, "The tag ".concat(tagName, " is unavailable")));
        return null;
      }
    }
  }, {
    key: "getTagObject",
    value: function getTagObject(item) {
      if (item instanceof Alias) return Alias;

      if (item.tag) {
        var match = this.tags.filter(function (t) {
          return t.tag === item.tag;
        });
        if (match.length > 0) return match.find(function (t) {
          return t.format === item.format;
        }) || match[0];
      }

      var tagObj, obj;

      if (item instanceof Scalar) {
        obj = item.value; // TODO: deprecate/remove class check

        var _match = this.tags.filter(function (t) {
          return t.identify && t.identify(obj) || t.class && obj instanceof t.class;
        });

        tagObj = _match.find(function (t) {
          return t.format === item.format;
        }) || _match.find(function (t) {
          return !t.format;
        });
      } else {
        obj = item;
        tagObj = this.tags.find(function (t) {
          return t.nodeClass && obj instanceof t.nodeClass;
        });
      }

      if (!tagObj) {
        var name = obj && obj.constructor ? obj.constructor.name : _typeof(obj);
        throw new Error("Tag not resolved for ".concat(name, " value"));
      }

      return tagObj;
    } // needs to be called before stringifier to allow for circular anchor refs

  }, {
    key: "stringifyProps",
    value: function stringifyProps(node, tagObj, _ref6) {
      var anchors = _ref6.anchors,
          doc = _ref6.doc;
      var props = [];
      var anchor = doc.anchors.getName(node);

      if (anchor) {
        anchors[anchor] = node;
        props.push("&".concat(anchor));
      }

      if (node.tag) {
        props.push(doc.stringifyTag(node.tag));
      } else if (!tagObj.default) {
        props.push(doc.stringifyTag(tagObj.tag));
      }

      return props.join(' ');
    }
  }, {
    key: "stringify",
    value: function stringify(item, ctx, onComment, onChompKeep) {
      var tagObj;

      if (!(item instanceof Node)) {
        var createCtx = {
          aliasNodes: [],
          onTagObj: function onTagObj(o) {
            return tagObj = o;
          },
          prevObjects: new Map()
        };
        item = this.createNode(item, true, null, createCtx);
        var anchors = ctx.doc.anchors;
        var _iteratorNormalCompletion2 = true;
        var _didIteratorError2 = false;
        var _iteratorError2 = undefined;

        try {
          for (var _iterator2 = createCtx.aliasNodes[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
            var alias = _step2.value;
            alias.source = alias.source.node;
            var name = anchors.getName(alias.source);

            if (!name) {
              name = anchors.newName();
              anchors.map[name] = alias.source;
            }
          }
        } catch (err) {
          _didIteratorError2 = true;
          _iteratorError2 = err;
        } finally {
          try {
            if (!_iteratorNormalCompletion2 && _iterator2.return != null) {
              _iterator2.return();
            }
          } finally {
            if (_didIteratorError2) {
              throw _iteratorError2;
            }
          }
        }
      }

      ctx.tags = this;
      if (item instanceof Pair) return item.toString(ctx, onComment, onChompKeep);
      if (!tagObj) tagObj = this.getTagObject(item);
      var props = this.stringifyProps(item, tagObj, ctx);
      if (props.length > 0) ctx.indentAtStart = (ctx.indentAtStart || 0) + props.length + 1;
      var str = typeof tagObj.stringify === 'function' ? tagObj.stringify(item, ctx, onComment, onChompKeep) : item instanceof Collection ? item.toString(ctx, onComment, onChompKeep) : stringifyString(item, ctx, onComment, onChompKeep);
      return props ? item instanceof Collection && str[0] !== '{' && str[0] !== '[' ? "".concat(props, "\n").concat(ctx.indent).concat(str) : "".concat(props, " ").concat(str) : str;
    }
  }]);

  return Schema;
}();

_defineProperty(Schema, "defaultPrefix", 'tag:yaml.org,2002:');

_defineProperty(Schema, "defaultTags", {
  MAP: 'tag:yaml.org,2002:map',
  SEQ: 'tag:yaml.org,2002:seq',
  STR: 'tag:yaml.org,2002:str'
});

export { Schema as default };