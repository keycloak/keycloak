import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _get from "@babel/runtime/helpers/get";
import _inherits from "@babel/runtime/helpers/inherits";
import { Type } from '../constants';
import Node from './Node';
import Range from './Range';
export var Chomp = {
  CLIP: 'CLIP',
  KEEP: 'KEEP',
  STRIP: 'STRIP'
};

var BlockValue = /*#__PURE__*/function (_Node) {
  _inherits(BlockValue, _Node);

  function BlockValue(type, props) {
    var _this;

    _classCallCheck(this, BlockValue);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(BlockValue).call(this, type, props));
    _this.blockIndent = null;
    _this.chomping = Chomp.CLIP;
    _this.header = null;
    return _this;
  }

  _createClass(BlockValue, [{
    key: "parseBlockHeader",
    value: function parseBlockHeader(start) {
      var src = this.context.src;
      var offset = start + 1;
      var bi = '';

      while (true) {
        var ch = src[offset];

        switch (ch) {
          case '-':
            this.chomping = Chomp.STRIP;
            break;

          case '+':
            this.chomping = Chomp.KEEP;
            break;

          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            bi += ch;
            break;

          default:
            this.blockIndent = Number(bi) || null;
            this.header = new Range(start, offset);
            return offset;
        }

        offset += 1;
      }
    }
  }, {
    key: "parseBlockValue",
    value: function parseBlockValue(start) {
      var _this$context = this.context,
          indent = _this$context.indent,
          src = _this$context.src;
      var offset = start;
      var valueEnd = start;
      var bi = this.blockIndent ? indent + this.blockIndent - 1 : indent;
      var minBlockIndent = 1;

      for (var ch = src[offset]; ch === '\n'; ch = src[offset]) {
        offset += 1;
        if (Node.atDocumentBoundary(src, offset)) break;
        var end = Node.endOfBlockIndent(src, bi, offset); // should not include tab?

        if (end === null) break;

        if (!this.blockIndent) {
          // no explicit block indent, none yet detected
          var lineIndent = end - (offset + indent);

          if (src[end] !== '\n') {
            // first line with non-whitespace content
            if (lineIndent < minBlockIndent) {
              offset -= 1;
              break;
            }

            this.blockIndent = lineIndent;
            bi = indent + this.blockIndent - 1;
          } else if (lineIndent > minBlockIndent) {
            // empty line with more whitespace
            minBlockIndent = lineIndent;
          }
        }

        if (src[end] === '\n') {
          offset = end;
        } else {
          offset = valueEnd = Node.endOfLine(src, end);
        }
      }

      if (this.chomping !== Chomp.KEEP) {
        offset = src[valueEnd] ? valueEnd + 1 : valueEnd;
      }

      this.valueRange = new Range(start + 1, offset);
      return offset;
    }
    /**
     * Parses a block value from the source
     *
     * Accepted forms are:
     * ```
     * BS
     * block
     * lines
     *
     * BS #comment
     * block
     * lines
     * ```
     * where the block style BS matches the regexp `[|>][-+1-9]*` and block lines
     * are empty or have an indent level greater than `indent`.
     *
     * @param {ParseContext} context
     * @param {number} start - Index of first character
     * @returns {number} - Index of the character after this block
     */

  }, {
    key: "parse",
    value: function parse(context, start) {
      this.context = context;
      var src = context.src;
      var offset = this.parseBlockHeader(start);
      offset = Node.endOfWhiteSpace(src, offset);
      offset = this.parseComment(offset);
      offset = this.parseBlockValue(offset);
      return offset;
    }
  }, {
    key: "setOrigRanges",
    value: function setOrigRanges(cr, offset) {
      offset = _get(_getPrototypeOf(BlockValue.prototype), "setOrigRanges", this).call(this, cr, offset);
      return this.header ? this.header.setOrigRange(cr, offset) : offset;
    }
  }, {
    key: "includesTrailingLines",
    get: function get() {
      return this.chomping === Chomp.KEEP;
    }
  }, {
    key: "strValue",
    get: function get() {
      if (!this.valueRange || !this.context) return null;
      var _this$valueRange = this.valueRange,
          start = _this$valueRange.start,
          end = _this$valueRange.end;
      var _this$context2 = this.context,
          indent = _this$context2.indent,
          src = _this$context2.src;
      if (this.valueRange.isEmpty()) return '';
      var lastNewLine = null;
      var ch = src[end - 1];

      while (ch === '\n' || ch === '\t' || ch === ' ') {
        end -= 1;

        if (end <= start) {
          if (this.chomping === Chomp.KEEP) break;else return ''; // probably never happens
        }

        if (ch === '\n') lastNewLine = end;
        ch = src[end - 1];
      }

      var keepStart = end + 1;

      if (lastNewLine) {
        if (this.chomping === Chomp.KEEP) {
          keepStart = lastNewLine;
          end = this.valueRange.end;
        } else {
          end = lastNewLine;
        }
      }

      var bi = indent + this.blockIndent;
      var folded = this.type === Type.BLOCK_FOLDED;
      var atStart = true;
      var str = '';
      var sep = '';
      var prevMoreIndented = false;

      for (var i = start; i < end; ++i) {
        for (var j = 0; j < bi; ++j) {
          if (src[i] !== ' ') break;
          i += 1;
        }

        var _ch = src[i];

        if (_ch === '\n') {
          if (sep === '\n') str += '\n';else sep = '\n';
        } else {
          var lineEnd = Node.endOfLine(src, i);
          var line = src.slice(i, lineEnd);
          i = lineEnd;

          if (folded && (_ch === ' ' || _ch === '\t') && i < keepStart) {
            if (sep === ' ') sep = '\n';else if (!prevMoreIndented && !atStart && sep === '\n') sep = '\n\n';
            str += sep + line; //+ ((lineEnd < end && src[lineEnd]) || '')

            sep = lineEnd < end && src[lineEnd] || '';
            prevMoreIndented = true;
          } else {
            str += sep + line;
            sep = folded && i < keepStart ? ' ' : '\n';
            prevMoreIndented = false;
          }

          if (atStart && line !== '') atStart = false;
        }
      }

      return this.chomping === Chomp.STRIP ? str : str + '\n';
    }
  }]);

  return BlockValue;
}(Node);

export { BlockValue as default };