"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _constants = require("../constants");

var _Node = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class BlankLine extends _Node.default {
  constructor() {
    super(_constants.Type.BLANK_LINE);
  }
  /* istanbul ignore next */


  get includesTrailingLines() {
    // This is never called from anywhere, but if it were,
    // this is the value it should return.
    return true;
  }
  /**
   * Parses blank lines from the source
   *
   * @param {ParseContext} context
   * @param {number} start - Index of first \n character
   * @returns {number} - Index of the character after this
   */


  parse(context, start) {
    this.context = context;
    const {
      src
    } = context;
    let offset = start + 1;

    while (_Node.default.atBlank(src, offset)) {
      const lineEnd = _Node.default.endOfWhiteSpace(src, offset);

      if (lineEnd === '\n') offset = lineEnd + 1;else break;
    }

    this.range = new _Range.default(start, offset);
    return offset;
  }

}

exports.default = BlankLine;