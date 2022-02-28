"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _Node = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class Alias extends _Node.default {
  /**
   * Parses an *alias from the source
   *
   * @param {ParseContext} context
   * @param {number} start - Index of first character
   * @returns {number} - Index of the character after this scalar
   */
  parse(context, start) {
    this.context = context;
    const {
      src
    } = context;

    let offset = _Node.default.endOfIdentifier(src, start + 1);

    this.valueRange = new _Range.default(start + 1, offset);
    offset = _Node.default.endOfWhiteSpace(src, offset);
    offset = this.parseComment(offset);
    return offset;
  }

}

exports.default = Alias;