"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _constants = require("../constants");

var _errors = require("../errors");

var _BlankLine = _interopRequireDefault(require("./BlankLine"));

var _Comment = _interopRequireDefault(require("./Comment"));

var _Node = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class FlowCollection extends _Node.default {
  constructor(type, props) {
    super(type, props);
    this.items = null;
  }

  prevNodeIsJsonLike(idx = this.items.length) {
    const node = this.items[idx - 1];
    return !!node && (node.jsonLike || node.type === _constants.Type.COMMENT && this.nodeIsJsonLike(idx - 1));
  }
  /**
   * @param {ParseContext} context
   * @param {number} start - Index of first character
   * @returns {number} - Index of the character after this
   */


  parse(context, start) {
    this.context = context;
    const {
      parseNode,
      src
    } = context;
    let {
      indent,
      lineStart
    } = context;
    let char = src[start]; // { or [

    this.items = [{
      char,
      offset: start
    }];

    let offset = _Node.default.endOfWhiteSpace(src, start + 1);

    char = src[offset];

    while (char && char !== ']' && char !== '}') {
      switch (char) {
        case '\n':
          {
            lineStart = offset + 1;

            const wsEnd = _Node.default.endOfWhiteSpace(src, lineStart);

            if (src[wsEnd] === '\n') {
              const blankLine = new _BlankLine.default();
              lineStart = blankLine.parse({
                src
              }, lineStart);
              this.items.push(blankLine);
            }

            offset = _Node.default.endOfIndent(src, lineStart);

            if (offset <= lineStart + indent) {
              char = src[offset];

              if (offset < lineStart + indent || char !== ']' && char !== '}') {
                const msg = 'Insufficient indentation in flow collection';
                this.error = new _errors.YAMLSemanticError(this, msg);
              }
            }
          }
          break;

        case ',':
          {
            this.items.push({
              char,
              offset
            });
            offset += 1;
          }
          break;

        case '#':
          {
            const comment = new _Comment.default();
            offset = comment.parse({
              src
            }, offset);
            this.items.push(comment);
          }
          break;

        case '?':
        case ':':
          {
            const next = src[offset + 1];

            if (next === '\n' || next === '\t' || next === ' ' || next === ',' || // in-flow : after JSON-like key does not need to be followed by whitespace
            char === ':' && this.prevNodeIsJsonLike()) {
              this.items.push({
                char,
                offset
              });
              offset += 1;
              break;
            }
          }
        // fallthrough

        default:
          {
            const node = parseNode({
              atLineStart: false,
              inCollection: false,
              inFlow: true,
              indent: -1,
              lineStart,
              parent: this
            }, offset);

            if (!node) {
              // at next document start
              this.valueRange = new _Range.default(start, offset);
              return offset;
            }

            this.items.push(node);
            offset = _Node.default.normalizeOffset(src, node.range.end);
          }
      }

      offset = _Node.default.endOfWhiteSpace(src, offset);
      char = src[offset];
    }

    this.valueRange = new _Range.default(start, offset + 1);

    if (char) {
      this.items.push({
        char,
        offset
      });
      offset = _Node.default.endOfWhiteSpace(src, offset + 1);
      offset = this.parseComment(offset);
    }

    return offset;
  }

  setOrigRanges(cr, offset) {
    offset = super.setOrigRanges(cr, offset);
    this.items.forEach(node => {
      if (node instanceof _Node.default) {
        offset = node.setOrigRanges(cr, offset);
      } else if (cr.length === 0) {
        node.origOffset = node.offset;
      } else {
        let i = offset;

        while (i < cr.length) {
          if (cr[i] > node.offset) break;else ++i;
        }

        node.origOffset = node.offset + i;
        offset = i;
      }
    });
    return offset;
  }

  toString() {
    const {
      context: {
        src
      },
      items,
      range,
      value
    } = this;
    if (value != null) return value;
    const nodes = items.filter(item => item instanceof _Node.default);
    let str = '';
    let prevEnd = range.start;
    nodes.forEach(node => {
      const prefix = src.slice(prevEnd, node.range.start);
      prevEnd = node.range.end;
      str += prefix + String(node);

      if (str[str.length - 1] === '\n' && src[prevEnd - 1] !== '\n' && src[prevEnd] === '\n') {
        // Comment range does not include the terminal newline, but its
        // stringified value does. Without this fix, newlines at comment ends
        // get duplicated.
        prevEnd += 1;
      }
    });
    str += src.slice(prevEnd, range.end);
    return _Node.default.addStringTerminator(src, range.end, str);
  }

}

exports.default = FlowCollection;