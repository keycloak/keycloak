import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";

var Range = /*#__PURE__*/function () {
  _createClass(Range, null, [{
    key: "copy",
    value: function copy(orig) {
      return new Range(orig.start, orig.end);
    }
  }]);

  function Range(start, end) {
    _classCallCheck(this, Range);

    this.start = start;
    this.end = end || start;
  }

  _createClass(Range, [{
    key: "isEmpty",
    value: function isEmpty() {
      return typeof this.start !== 'number' || !this.end || this.end <= this.start;
    }
    /**
     * Set `origStart` and `origEnd` to point to the original source range for
     * this node, which may differ due to dropped CR characters.
     *
     * @param {number[]} cr - Positions of dropped CR characters
     * @param {number} offset - Starting index of `cr` from the last call
     * @returns {number} - The next offset, matching the one found for `origStart`
     */

  }, {
    key: "setOrigRange",
    value: function setOrigRange(cr, offset) {
      var start = this.start,
          end = this.end;

      if (cr.length === 0 || end <= cr[0]) {
        this.origStart = start;
        this.origEnd = end;
        return offset;
      }

      var i = offset;

      while (i < cr.length) {
        if (cr[i] > start) break;else ++i;
      }

      this.origStart = start + i;
      var nextOffset = i;

      while (i < cr.length) {
        // if end was at \n, it should now be at \r
        if (cr[i] >= end) break;else ++i;
      }

      this.origEnd = end + i;
      return nextOffset;
    }
  }]);

  return Range;
}();

export { Range as default };