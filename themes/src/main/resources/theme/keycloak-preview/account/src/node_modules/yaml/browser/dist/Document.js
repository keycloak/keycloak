import _slicedToArray from "@babel/runtime/helpers/slicedToArray";
import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _defineProperty from "@babel/runtime/helpers/defineProperty";
import addComment from './addComment';
import Anchors from './Anchors';
import { Char, Type } from './constants';
import { YAMLError, YAMLReferenceError, YAMLSemanticError, YAMLSyntaxError, YAMLWarning } from './errors';
import listTagNames from './listTagNames';
import Schema from './schema';
import Alias from './schema/Alias';
import Collection, { isEmptyPath } from './schema/Collection';
import Node from './schema/Node';
import Scalar from './schema/Scalar';
import _toJSON from './toJSON';

var isCollectionItem = function isCollectionItem(node) {
  return node && [Type.MAP_KEY, Type.MAP_VALUE, Type.SEQ_ITEM].includes(node.type);
};

var Document = /*#__PURE__*/function () {
  function Document(options) {
    _classCallCheck(this, Document);

    this.anchors = new Anchors(options.anchorPrefix);
    this.commentBefore = null;
    this.comment = null;
    this.contents = null;
    this.directivesEndMarker = null;
    this.errors = [];
    this.options = options;
    this.schema = null;
    this.tagPrefixes = [];
    this.version = null;
    this.warnings = [];
  }

  _createClass(Document, [{
    key: "assertCollectionContents",
    value: function assertCollectionContents() {
      if (this.contents instanceof Collection) return true;
      throw new Error('Expected a YAML collection as document contents');
    }
  }, {
    key: "add",
    value: function add(value) {
      this.assertCollectionContents();
      return this.contents.add(value);
    }
  }, {
    key: "addIn",
    value: function addIn(path, value) {
      this.assertCollectionContents();
      this.contents.addIn(path, value);
    }
  }, {
    key: "delete",
    value: function _delete(key) {
      this.assertCollectionContents();
      return this.contents.delete(key);
    }
  }, {
    key: "deleteIn",
    value: function deleteIn(path) {
      if (isEmptyPath(path)) {
        if (this.contents == null) return false;
        this.contents = null;
        return true;
      }

      this.assertCollectionContents();
      return this.contents.deleteIn(path);
    }
  }, {
    key: "getDefaults",
    value: function getDefaults() {
      return Document.defaults[this.version] || Document.defaults[this.options.version] || {};
    }
  }, {
    key: "get",
    value: function get(key, keepScalar) {
      return this.contents instanceof Collection ? this.contents.get(key, keepScalar) : undefined;
    }
  }, {
    key: "getIn",
    value: function getIn(path, keepScalar) {
      if (isEmptyPath(path)) return !keepScalar && this.contents instanceof Scalar ? this.contents.value : this.contents;
      return this.contents instanceof Collection ? this.contents.getIn(path, keepScalar) : undefined;
    }
  }, {
    key: "has",
    value: function has(key) {
      return this.contents instanceof Collection ? this.contents.has(key) : false;
    }
  }, {
    key: "hasIn",
    value: function hasIn(path) {
      if (isEmptyPath(path)) return this.contents !== undefined;
      return this.contents instanceof Collection ? this.contents.hasIn(path) : false;
    }
  }, {
    key: "set",
    value: function set(key, value) {
      this.assertCollectionContents();
      this.contents.set(key, value);
    }
  }, {
    key: "setIn",
    value: function setIn(path, value) {
      if (isEmptyPath(path)) this.contents = value;else {
        this.assertCollectionContents();
        this.contents.setIn(path, value);
      }
    }
  }, {
    key: "setSchema",
    value: function setSchema(id, customTags) {
      if (!id && !customTags && this.schema) return;
      if (typeof id === 'number') id = id.toFixed(1);

      if (id === '1.0' || id === '1.1' || id === '1.2') {
        if (this.version) this.version = id;else this.options.version = id;
        delete this.options.schema;
      } else if (id && typeof id === 'string') {
        this.options.schema = id;
      }

      if (Array.isArray(customTags)) this.options.customTags = customTags;
      var opt = Object.assign({}, this.getDefaults(), this.options);
      this.schema = new Schema(opt);
    }
  }, {
    key: "parse",
    value: function parse(node, prevDoc) {
      if (this.options.keepCstNodes) this.cstNode = node;
      if (this.options.keepNodeTypes) this.type = 'DOCUMENT';
      var _node$directives = node.directives,
          directives = _node$directives === void 0 ? [] : _node$directives,
          _node$contents = node.contents,
          contents = _node$contents === void 0 ? [] : _node$contents,
          directivesEndMarker = node.directivesEndMarker,
          error = node.error,
          valueRange = node.valueRange;

      if (error) {
        if (!error.source) error.source = this;
        this.errors.push(error);
      }

      this.parseDirectives(directives, prevDoc);
      if (directivesEndMarker) this.directivesEndMarker = true;
      this.range = valueRange ? [valueRange.start, valueRange.end] : null;
      this.setSchema();
      this.anchors._cstAliases = [];
      this.parseContents(contents);
      this.anchors.resolveNodes();

      if (this.options.prettyErrors) {
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
          for (var _iterator = this.errors[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
            var _error = _step.value;
            if (_error instanceof YAMLError) _error.makePretty();
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

        var _iteratorNormalCompletion2 = true;
        var _didIteratorError2 = false;
        var _iteratorError2 = undefined;

        try {
          for (var _iterator2 = this.warnings[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
            var warn = _step2.value;
            if (warn instanceof YAMLError) warn.makePretty();
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

      return this;
    }
  }, {
    key: "parseDirectives",
    value: function parseDirectives(directives, prevDoc) {
      var _this = this;

      var directiveComments = [];
      var hasDirectives = false;
      directives.forEach(function (directive) {
        var comment = directive.comment,
            name = directive.name;

        switch (name) {
          case 'TAG':
            _this.resolveTagDirective(directive);

            hasDirectives = true;
            break;

          case 'YAML':
          case 'YAML:1.0':
            _this.resolveYamlDirective(directive);

            hasDirectives = true;
            break;

          default:
            if (name) {
              var msg = "YAML only supports %TAG and %YAML directives, and not %".concat(name);

              _this.warnings.push(new YAMLWarning(directive, msg));
            }

        }

        if (comment) directiveComments.push(comment);
      });

      if (prevDoc && !hasDirectives && '1.1' === (this.version || prevDoc.version || this.options.version)) {
        var copyTagPrefix = function copyTagPrefix(_ref) {
          var handle = _ref.handle,
              prefix = _ref.prefix;
          return {
            handle: handle,
            prefix: prefix
          };
        };

        this.tagPrefixes = prevDoc.tagPrefixes.map(copyTagPrefix);
        this.version = prevDoc.version;
      }

      this.commentBefore = directiveComments.join('\n') || null;
    }
  }, {
    key: "parseContents",
    value: function parseContents(contents) {
      var _this2 = this;

      var comments = {
        before: [],
        after: []
      };
      var contentNodes = [];
      var spaceBefore = false;
      contents.forEach(function (node) {
        if (node.valueRange) {
          if (contentNodes.length === 1) {
            var msg = 'Document is not valid YAML (bad indentation?)';

            _this2.errors.push(new YAMLSyntaxError(node, msg));
          }

          var res = _this2.resolveNode(node);

          if (spaceBefore) {
            res.spaceBefore = true;
            spaceBefore = false;
          }

          contentNodes.push(res);
        } else if (node.comment !== null) {
          var cc = contentNodes.length === 0 ? comments.before : comments.after;
          cc.push(node.comment);
        } else if (node.type === Type.BLANK_LINE) {
          spaceBefore = true;

          if (contentNodes.length === 0 && comments.before.length > 0 && !_this2.commentBefore) {
            // space-separated comments at start are parsed as document comments
            _this2.commentBefore = comments.before.join('\n');
            comments.before = [];
          }
        }
      });

      switch (contentNodes.length) {
        case 0:
          this.contents = null;
          comments.after = comments.before;
          break;

        case 1:
          this.contents = contentNodes[0];

          if (this.contents) {
            var cb = comments.before.join('\n') || null;

            if (cb) {
              var cbNode = this.contents instanceof Collection && this.contents.items[0] ? this.contents.items[0] : this.contents;
              cbNode.commentBefore = cbNode.commentBefore ? "".concat(cb, "\n").concat(cbNode.commentBefore) : cb;
            }
          } else {
            comments.after = comments.before.concat(comments.after);
          }

          break;

        default:
          this.contents = contentNodes;

          if (this.contents[0]) {
            this.contents[0].commentBefore = comments.before.join('\n') || null;
          } else {
            comments.after = comments.before.concat(comments.after);
          }

      }

      this.comment = comments.after.join('\n') || null;
    }
  }, {
    key: "resolveTagDirective",
    value: function resolveTagDirective(directive) {
      var _directive$parameters = _slicedToArray(directive.parameters, 2),
          handle = _directive$parameters[0],
          prefix = _directive$parameters[1];

      if (handle && prefix) {
        if (this.tagPrefixes.every(function (p) {
          return p.handle !== handle;
        })) {
          this.tagPrefixes.push({
            handle: handle,
            prefix: prefix
          });
        } else {
          var msg = 'The %TAG directive must only be given at most once per handle in the same document.';
          this.errors.push(new YAMLSemanticError(directive, msg));
        }
      } else {
        var _msg = 'Insufficient parameters given for %TAG directive';
        this.errors.push(new YAMLSemanticError(directive, _msg));
      }
    }
  }, {
    key: "resolveYamlDirective",
    value: function resolveYamlDirective(directive) {
      var _directive$parameters2 = _slicedToArray(directive.parameters, 1),
          version = _directive$parameters2[0];

      if (directive.name === 'YAML:1.0') version = '1.0';

      if (this.version) {
        var msg = 'The %YAML directive must only be given at most once per document.';
        this.errors.push(new YAMLSemanticError(directive, msg));
      }

      if (!version) {
        var _msg2 = 'Insufficient parameters given for %YAML directive';
        this.errors.push(new YAMLSemanticError(directive, _msg2));
      } else {
        if (!Document.defaults[version]) {
          var v0 = this.version || this.options.version;

          var _msg3 = "Document will be parsed as YAML ".concat(v0, " rather than YAML ").concat(version);

          this.warnings.push(new YAMLWarning(directive, _msg3));
        }

        this.version = version;
      }
    }
  }, {
    key: "resolveTagName",
    value: function resolveTagName(node) {
      var tag = node.tag,
          type = node.type;
      var nonSpecific = false;

      if (tag) {
        var handle = tag.handle,
            suffix = tag.suffix,
            verbatim = tag.verbatim;

        if (verbatim) {
          if (verbatim !== '!' && verbatim !== '!!') return verbatim;
          var msg = "Verbatim tags aren't resolved, so ".concat(verbatim, " is invalid.");
          this.errors.push(new YAMLSemanticError(node, msg));
        } else if (handle === '!' && !suffix) {
          nonSpecific = true;
        } else {
          var prefix = this.tagPrefixes.find(function (p) {
            return p.handle === handle;
          });

          if (!prefix) {
            var dtp = this.getDefaults().tagPrefixes;
            if (dtp) prefix = dtp.find(function (p) {
              return p.handle === handle;
            });
          }

          if (prefix) {
            if (suffix) {
              if (handle === '!' && (this.version || this.options.version) === '1.0') {
                if (suffix[0] === '^') return suffix;

                if (/[:/]/.test(suffix)) {
                  // word/foo -> tag:word.yaml.org,2002:foo
                  var vocab = suffix.match(/^([a-z0-9-]+)\/(.*)/i);
                  return vocab ? "tag:".concat(vocab[1], ".yaml.org,2002:").concat(vocab[2]) : "tag:".concat(suffix);
                }
              }

              return prefix.prefix + decodeURIComponent(suffix);
            }

            this.errors.push(new YAMLSemanticError(node, "The ".concat(handle, " tag has no suffix.")));
          } else {
            var _msg4 = "The ".concat(handle, " tag handle is non-default and was not declared.");

            this.errors.push(new YAMLSemanticError(node, _msg4));
          }
        }
      }

      switch (type) {
        case Type.BLOCK_FOLDED:
        case Type.BLOCK_LITERAL:
        case Type.QUOTE_DOUBLE:
        case Type.QUOTE_SINGLE:
          return Schema.defaultTags.STR;

        case Type.FLOW_MAP:
        case Type.MAP:
          return Schema.defaultTags.MAP;

        case Type.FLOW_SEQ:
        case Type.SEQ:
          return Schema.defaultTags.SEQ;

        case Type.PLAIN:
          return nonSpecific ? Schema.defaultTags.STR : null;

        default:
          return null;
      }
    }
  }, {
    key: "resolveNode",
    value: function resolveNode(node) {
      if (!node) return null;
      var anchors = this.anchors,
          errors = this.errors,
          schema = this.schema;
      var hasAnchor = false;
      var hasTag = false;
      var comments = {
        before: [],
        after: []
      };
      var props = isCollectionItem(node.context.parent) ? node.context.parent.props.concat(node.props) : node.props;
      var _iteratorNormalCompletion3 = true;
      var _didIteratorError3 = false;
      var _iteratorError3 = undefined;

      try {
        for (var _iterator3 = props[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {
          var _step3$value = _step3.value,
              start = _step3$value.start,
              end = _step3$value.end;

          switch (node.context.src[start]) {
            case Char.COMMENT:
              {
                if (!node.commentHasRequiredWhitespace(start)) {
                  var _msg7 = 'Comments must be separated from other tokens by white space characters';
                  errors.push(new YAMLSemanticError(node, _msg7));
                }

                var c = node.context.src.slice(start + 1, end);
                var header = node.header,
                    valueRange = node.valueRange;

                if (valueRange && (start > valueRange.start || header && start > header.start)) {
                  comments.after.push(c);
                } else {
                  comments.before.push(c);
                }
              }
              break;

            case Char.ANCHOR:
              if (hasAnchor) {
                var _msg8 = 'A node can have at most one anchor';
                errors.push(new YAMLSemanticError(node, _msg8));
              }

              hasAnchor = true;
              break;

            case Char.TAG:
              if (hasTag) {
                var _msg9 = 'A node can have at most one tag';
                errors.push(new YAMLSemanticError(node, _msg9));
              }

              hasTag = true;
              break;
          }
        }
      } catch (err) {
        _didIteratorError3 = true;
        _iteratorError3 = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion3 && _iterator3.return != null) {
            _iterator3.return();
          }
        } finally {
          if (_didIteratorError3) {
            throw _iteratorError3;
          }
        }
      }

      if (hasAnchor) {
        var name = node.anchor;
        var prev = anchors.getNode(name); // At this point, aliases for any preceding node with the same anchor
        // name have already been resolved, so it may safely be renamed.

        if (prev) anchors.map[anchors.newName(name)] = prev; // During parsing, we need to store the CST node in anchors.map as
        // anchors need to be available during resolution to allow for
        // circular references.

        anchors.map[name] = node;
      }

      var res;

      if (node.type === Type.ALIAS) {
        if (hasAnchor || hasTag) {
          var msg = 'An alias node must not specify any properties';
          errors.push(new YAMLSemanticError(node, msg));
        }

        var _name = node.rawValue;
        var src = anchors.getNode(_name);

        if (!src) {
          var _msg5 = "Aliased anchor not found: ".concat(_name);

          errors.push(new YAMLReferenceError(node, _msg5));
          return null;
        } // Lazy resolution for circular references


        res = new Alias(src);

        anchors._cstAliases.push(res);
      } else {
        var tagName = this.resolveTagName(node);

        if (tagName) {
          res = schema.resolveNodeWithFallback(this, node, tagName);
        } else {
          if (node.type !== Type.PLAIN) {
            var _msg6 = "Failed to resolve ".concat(node.type, " node here");

            errors.push(new YAMLSyntaxError(node, _msg6));
            return null;
          }

          try {
            res = schema.resolveScalar(node.strValue || '');
          } catch (error) {
            if (!error.source) error.source = node;
            errors.push(error);
            return null;
          }
        }
      }

      if (res) {
        res.range = [node.range.start, node.range.end];
        if (this.options.keepCstNodes) res.cstNode = node;
        if (this.options.keepNodeTypes) res.type = node.type;
        var cb = comments.before.join('\n');

        if (cb) {
          res.commentBefore = res.commentBefore ? "".concat(res.commentBefore, "\n").concat(cb) : cb;
        }

        var ca = comments.after.join('\n');
        if (ca) res.comment = res.comment ? "".concat(res.comment, "\n").concat(ca) : ca;
      }

      return node.resolved = res;
    }
  }, {
    key: "listNonDefaultTags",
    value: function listNonDefaultTags() {
      return listTagNames(this.contents).filter(function (t) {
        return t.indexOf(Schema.defaultPrefix) !== 0;
      });
    }
  }, {
    key: "setTagPrefix",
    value: function setTagPrefix(handle, prefix) {
      if (handle[0] !== '!' || handle[handle.length - 1] !== '!') throw new Error('Handle must start and end with !');

      if (prefix) {
        var prev = this.tagPrefixes.find(function (p) {
          return p.handle === handle;
        });
        if (prev) prev.prefix = prefix;else this.tagPrefixes.push({
          handle: handle,
          prefix: prefix
        });
      } else {
        this.tagPrefixes = this.tagPrefixes.filter(function (p) {
          return p.handle !== handle;
        });
      }
    }
  }, {
    key: "stringifyTag",
    value: function stringifyTag(tag) {
      if ((this.version || this.options.version) === '1.0') {
        var priv = tag.match(/^tag:private\.yaml\.org,2002:([^:/]+)$/);
        if (priv) return '!' + priv[1];
        var vocab = tag.match(/^tag:([a-zA-Z0-9-]+)\.yaml\.org,2002:(.*)/);
        return vocab ? "!".concat(vocab[1], "/").concat(vocab[2]) : "!".concat(tag.replace(/^tag:/, ''));
      } else {
        var p = this.tagPrefixes.find(function (p) {
          return tag.indexOf(p.prefix) === 0;
        });

        if (!p) {
          var dtp = this.getDefaults().tagPrefixes;
          p = dtp && dtp.find(function (p) {
            return tag.indexOf(p.prefix) === 0;
          });
        }

        if (!p) return tag[0] === '!' ? tag : "!<".concat(tag, ">");
        var suffix = tag.substr(p.prefix.length).replace(/[!,[\]{}]/g, function (ch) {
          return {
            '!': '%21',
            ',': '%2C',
            '[': '%5B',
            ']': '%5D',
            '{': '%7B',
            '}': '%7D'
          }[ch];
        });
        return p.handle + suffix;
      }
    }
  }, {
    key: "toJSON",
    value: function toJSON(arg) {
      var _this3 = this;

      var _this$options = this.options,
          keepBlobsInJSON = _this$options.keepBlobsInJSON,
          mapAsMap = _this$options.mapAsMap,
          maxAliasCount = _this$options.maxAliasCount;
      var keep = keepBlobsInJSON && (typeof arg !== 'string' || !(this.contents instanceof Scalar));
      var ctx = {
        doc: this,
        keep: keep,
        mapAsMap: keep && !!mapAsMap,
        maxAliasCount: maxAliasCount
      };
      var anchorNames = Object.keys(this.anchors.map);
      if (anchorNames.length > 0) ctx.anchors = anchorNames.map(function (name) {
        return {
          alias: [],
          aliasCount: 0,
          count: 1,
          node: _this3.anchors.map[name]
        };
      });
      return _toJSON(this.contents, arg, ctx);
    }
  }, {
    key: "toString",
    value: function toString() {
      if (this.errors.length > 0) throw new Error('Document with errors cannot be stringified');
      this.setSchema();
      var lines = [];
      var hasDirectives = false;

      if (this.version) {
        var vd = '%YAML 1.2';

        if (this.schema.name === 'yaml-1.1') {
          if (this.version === '1.0') vd = '%YAML:1.0';else if (this.version === '1.1') vd = '%YAML 1.1';
        }

        lines.push(vd);
        hasDirectives = true;
      }

      var tagNames = this.listNonDefaultTags();
      this.tagPrefixes.forEach(function (_ref2) {
        var handle = _ref2.handle,
            prefix = _ref2.prefix;

        if (tagNames.some(function (t) {
          return t.indexOf(prefix) === 0;
        })) {
          lines.push("%TAG ".concat(handle, " ").concat(prefix));
          hasDirectives = true;
        }
      });
      if (hasDirectives || this.directivesEndMarker) lines.push('---');

      if (this.commentBefore) {
        if (hasDirectives || !this.directivesEndMarker) lines.unshift('');
        lines.unshift(this.commentBefore.replace(/^/gm, '#'));
      }

      var ctx = {
        anchors: {},
        doc: this,
        indent: ''
      };
      var chompKeep = false;
      var contentComment = null;

      if (this.contents) {
        if (this.contents instanceof Node) {
          if (this.contents.spaceBefore && (hasDirectives || this.directivesEndMarker)) lines.push('');
          if (this.contents.commentBefore) lines.push(this.contents.commentBefore.replace(/^/gm, '#')); // top-level block scalars need to be indented if followed by a comment

          ctx.forceBlockIndent = !!this.comment;
          contentComment = this.contents.comment;
        }

        var onChompKeep = contentComment ? null : function () {
          return chompKeep = true;
        };
        var body = this.schema.stringify(this.contents, ctx, function () {
          return contentComment = null;
        }, onChompKeep);
        lines.push(addComment(body, '', contentComment));
      } else if (this.contents !== undefined) {
        lines.push(this.schema.stringify(this.contents, ctx));
      }

      if (this.comment) {
        if ((!chompKeep || contentComment) && lines[lines.length - 1] !== '') lines.push('');
        lines.push(this.comment.replace(/^/gm, '#'));
      }

      return lines.join('\n') + '\n';
    }
  }]);

  return Document;
}();

_defineProperty(Document, "defaults", {
  '1.0': {
    schema: 'yaml-1.1',
    merge: true,
    tagPrefixes: [{
      handle: '!',
      prefix: Schema.defaultPrefix
    }, {
      handle: '!!',
      prefix: 'tag:private.yaml.org,2002:'
    }]
  },
  '1.1': {
    schema: 'yaml-1.1',
    merge: true,
    tagPrefixes: [{
      handle: '!',
      prefix: '!'
    }, {
      handle: '!!',
      prefix: Schema.defaultPrefix
    }]
  },
  '1.2': {
    schema: 'core',
    merge: false,
    tagPrefixes: [{
      handle: '!',
      prefix: '!'
    }, {
      handle: '!!',
      prefix: Schema.defaultPrefix
    }]
  }
});

export { Document as default };