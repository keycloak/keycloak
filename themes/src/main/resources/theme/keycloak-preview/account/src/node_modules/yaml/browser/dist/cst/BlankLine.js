import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
import { Type } from '../constants';
import Node from './Node';
import Range from './Range';

var BlankLine = /*#__PURE__*/function (_Node) {
  _inherits(BlankLine, _Node);

  function BlankLine() {
    _classCallCheck(this, BlankLine);

    return _possibleConstructorReturn(this, _getPrototypeOf(BlankLine).call(this, Type.BLANK_LINE));
  }
  /* istanbul ignore next */


  _createClass(BlankLine, [{
    key: "parse",

    /**
     * Parses blank lines from the source
     *
     * @param {ParseContext} context
     * @param {number} start - Index of first \n character
     * @returns {number} - Index of the character after this
     */
    value: function parse(context, start) {
      this.context = context;
      var src = context.src;
      var offset = start + 1;

      while (Node.atBlank(src, offset)) {
        var lineEnd = Node.endOfWhiteSpace(src, offset);
        if (lineEnd === '\n') offset = lineEnd + 1;else break;
      }

      this.range = new Range(start, offset);
      return offset;
    }
  }, {
    key: "includesTrailingLines",
    get: function get() {
      // This is never called from anywhere, but if it were,
      // this is the value it should return.
      return true;
    }
  }]);

  return BlankLine;
}(Node);

export { BlankLine as default };