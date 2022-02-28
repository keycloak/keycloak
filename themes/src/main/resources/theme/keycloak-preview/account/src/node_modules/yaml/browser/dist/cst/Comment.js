import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
import { Type } from '../constants';
import Node from './Node';
import Range from './Range';

var Comment = /*#__PURE__*/function (_Node) {
  _inherits(Comment, _Node);

  function Comment() {
    _classCallCheck(this, Comment);

    return _possibleConstructorReturn(this, _getPrototypeOf(Comment).call(this, Type.COMMENT));
  }
  /**
   * Parses a comment line from the source
   *
   * @param {ParseContext} context
   * @param {number} start - Index of first character
   * @returns {number} - Index of the character after this scalar
   */


  _createClass(Comment, [{
    key: "parse",
    value: function parse(context, start) {
      this.context = context;
      var offset = this.parseComment(start);
      this.range = new Range(start, offset);
      return offset;
    }
  }]);

  return Comment;
}(Node);

export { Comment as default };