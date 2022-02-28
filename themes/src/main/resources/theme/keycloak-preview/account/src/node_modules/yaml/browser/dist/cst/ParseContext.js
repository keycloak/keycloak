import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _defineProperty from "@babel/runtime/helpers/defineProperty";
import { Char, Type } from '../constants';
import { YAMLSyntaxError } from '../errors';
import Alias from './Alias';
import BlockValue from './BlockValue';
import Collection from './Collection';
import CollectionItem from './CollectionItem';
import FlowCollection from './FlowCollection';
import Node from './Node';
import PlainValue from './PlainValue';
import QuoteDouble from './QuoteDouble';
import QuoteSingle from './QuoteSingle';
import Range from './Range';

function createNewNode(type, props) {
  switch (type) {
    case Type.ALIAS:
      return new Alias(type, props);

    case Type.BLOCK_FOLDED:
    case Type.BLOCK_LITERAL:
      return new BlockValue(type, props);

    case Type.FLOW_MAP:
    case Type.FLOW_SEQ:
      return new FlowCollection(type, props);

    case Type.MAP_KEY:
    case Type.MAP_VALUE:
    case Type.SEQ_ITEM:
      return new CollectionItem(type, props);

    case Type.COMMENT:
    case Type.PLAIN:
      return new PlainValue(type, props);

    case Type.QUOTE_DOUBLE:
      return new QuoteDouble(type, props);

    case Type.QUOTE_SINGLE:
      return new QuoteSingle(type, props);

    /* istanbul ignore next */

    default:
      return null;
    // should never happen
  }
}
/**
 * @param {boolean} atLineStart - Node starts at beginning of line
 * @param {boolean} inFlow - true if currently in a flow context
 * @param {boolean} inCollection - true if currently in a collection context
 * @param {number} indent - Current level of indentation
 * @param {number} lineStart - Start of the current line
 * @param {Node} parent - The parent of the node
 * @param {string} src - Source of the YAML document
 */


var ParseContext = /*#__PURE__*/function () {
  _createClass(ParseContext, null, [{
    key: "parseType",
    value: function parseType(src, offset, inFlow) {
      switch (src[offset]) {
        case '*':
          return Type.ALIAS;

        case '>':
          return Type.BLOCK_FOLDED;

        case '|':
          return Type.BLOCK_LITERAL;

        case '{':
          return Type.FLOW_MAP;

        case '[':
          return Type.FLOW_SEQ;

        case '?':
          return !inFlow && Node.atBlank(src, offset + 1, true) ? Type.MAP_KEY : Type.PLAIN;

        case ':':
          return !inFlow && Node.atBlank(src, offset + 1, true) ? Type.MAP_VALUE : Type.PLAIN;

        case '-':
          return !inFlow && Node.atBlank(src, offset + 1, true) ? Type.SEQ_ITEM : Type.PLAIN;

        case '"':
          return Type.QUOTE_DOUBLE;

        case "'":
          return Type.QUOTE_SINGLE;

        default:
          return Type.PLAIN;
      }
    }
  }]);

  function ParseContext() {
    var _this = this;

    var orig = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

    var _ref = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
        atLineStart = _ref.atLineStart,
        inCollection = _ref.inCollection,
        inFlow = _ref.inFlow,
        indent = _ref.indent,
        lineStart = _ref.lineStart,
        parent = _ref.parent;

    _classCallCheck(this, ParseContext);

    _defineProperty(this, "parseNode", function (overlay, start) {
      if (Node.atDocumentBoundary(_this.src, start)) return null;
      var context = new ParseContext(_this, overlay);

      var _context$parseProps = context.parseProps(start),
          props = _context$parseProps.props,
          type = _context$parseProps.type,
          valueStart = _context$parseProps.valueStart;

      var node = createNewNode(type, props);
      var offset = node.parse(context, valueStart);
      node.range = new Range(start, offset);
      /* istanbul ignore if */

      if (offset <= start) {
        // This should never happen, but if it does, let's make sure to at least
        // step one character forward to avoid a busy loop.
        node.error = new Error("Node#parse consumed no characters");
        node.error.parseEnd = offset;
        node.error.source = node;
        node.range.end = start + 1;
      }

      if (context.nodeStartsCollection(node)) {
        if (!node.error && !context.atLineStart && context.parent.type === Type.DOCUMENT) {
          node.error = new YAMLSyntaxError(node, 'Block collection must not have preceding content here (e.g. directives-end indicator)');
        }

        var collection = new Collection(node);
        offset = collection.parse(new ParseContext(context), offset);
        collection.range = new Range(start, offset);
        return collection;
      }

      return node;
    });

    this.atLineStart = atLineStart != null ? atLineStart : orig.atLineStart || false;
    this.inCollection = inCollection != null ? inCollection : orig.inCollection || false;
    this.inFlow = inFlow != null ? inFlow : orig.inFlow || false;
    this.indent = indent != null ? indent : orig.indent;
    this.lineStart = lineStart != null ? lineStart : orig.lineStart;
    this.parent = parent != null ? parent : orig.parent || {};
    this.root = orig.root;
    this.src = orig.src;
  }

  _createClass(ParseContext, [{
    key: "nodeStartsCollection",
    value: function nodeStartsCollection(node) {
      var inCollection = this.inCollection,
          inFlow = this.inFlow,
          src = this.src;
      if (inCollection || inFlow) return false;
      if (node instanceof CollectionItem) return true; // check for implicit key

      var offset = node.range.end;
      if (src[offset] === '\n' || src[offset - 1] === '\n') return false;
      offset = Node.endOfWhiteSpace(src, offset);
      return src[offset] === ':';
    } // Anchor and tag are before type, which determines the node implementation
    // class; hence this intermediate step.

  }, {
    key: "parseProps",
    value: function parseProps(offset) {
      var inFlow = this.inFlow,
          parent = this.parent,
          src = this.src;
      var props = [];
      var lineHasProps = false;
      offset = Node.endOfWhiteSpace(src, offset);
      var ch = src[offset];

      while (ch === Char.ANCHOR || ch === Char.COMMENT || ch === Char.TAG || ch === '\n') {
        if (ch === '\n') {
          var lineStart = offset + 1;
          var inEnd = Node.endOfIndent(src, lineStart);
          var indentDiff = inEnd - (lineStart + this.indent);
          var noIndicatorAsIndent = parent.type === Type.SEQ_ITEM && parent.context.atLineStart;
          if (!Node.nextNodeIsIndented(src[inEnd], indentDiff, !noIndicatorAsIndent)) break;
          this.atLineStart = true;
          this.lineStart = lineStart;
          lineHasProps = false;
          offset = inEnd;
        } else if (ch === Char.COMMENT) {
          var end = Node.endOfLine(src, offset + 1);
          props.push(new Range(offset, end));
          offset = end;
        } else {
          var _end = Node.endOfIdentifier(src, offset + 1);

          if (ch === Char.TAG && src[_end] === ',' && /^[a-zA-Z0-9-]+\.[a-zA-Z0-9-]+,\d\d\d\d(-\d\d){0,2}\/\S/.test(src.slice(offset + 1, _end + 13))) {
            // Let's presume we're dealing with a YAML 1.0 domain tag here, rather
            // than an empty but 'foo.bar' private-tagged node in a flow collection
            // followed without whitespace by a plain string starting with a year
            // or date divided by something.
            _end = Node.endOfIdentifier(src, _end + 5);
          }

          props.push(new Range(offset, _end));
          lineHasProps = true;
          offset = Node.endOfWhiteSpace(src, _end);
        }

        ch = src[offset];
      } // '- &a : b' has an anchor on an empty node


      if (lineHasProps && ch === ':' && Node.atBlank(src, offset + 1, true)) offset -= 1;
      var type = ParseContext.parseType(src, offset, inFlow);
      return {
        props: props,
        type: type,
        valueStart: offset
      };
    }
    /**
     * Parses a node from the source
     * @param {ParseContext} overlay
     * @param {number} start - Index of first non-whitespace character for the node
     * @returns {?Node} - null if at a document boundary
     */

  }]);

  return ParseContext;
}();

export { ParseContext as default };