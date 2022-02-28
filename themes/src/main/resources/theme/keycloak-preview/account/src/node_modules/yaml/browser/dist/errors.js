import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
import _wrapNativeSuper from "@babel/runtime/helpers/wrapNativeSuper";
import Node from './cst/Node';
import { getLinePos, getPrettyContext } from './cst/source-utils';
import Range from './cst/Range';
export var YAMLError = /*#__PURE__*/function (_Error) {
  _inherits(YAMLError, _Error);

  function YAMLError(name, source, message) {
    var _this;

    _classCallCheck(this, YAMLError);

    if (!message || !(source instanceof Node)) throw new Error("Invalid arguments for new ".concat(name));
    _this = _possibleConstructorReturn(this, _getPrototypeOf(YAMLError).call(this));
    _this.name = name;
    _this.message = message;
    _this.source = source;
    return _this;
  }

  _createClass(YAMLError, [{
    key: "makePretty",
    value: function makePretty() {
      if (!this.source) return;
      this.nodeType = this.source.type;
      var cst = this.source.context && this.source.context.root;

      if (typeof this.offset === 'number') {
        this.range = new Range(this.offset, this.offset + 1);
        var start = cst && getLinePos(this.offset, cst);

        if (start) {
          var end = {
            line: start.line,
            col: start.col + 1
          };
          this.linePos = {
            start: start,
            end: end
          };
        }

        delete this.offset;
      } else {
        this.range = this.source.range;
        this.linePos = this.source.rangeAsLinePos;
      }

      if (this.linePos) {
        var _this$linePos$start = this.linePos.start,
            line = _this$linePos$start.line,
            col = _this$linePos$start.col;
        this.message += " at line ".concat(line, ", column ").concat(col);
        var ctx = cst && getPrettyContext(this.linePos, cst);
        if (ctx) this.message += ":\n\n".concat(ctx, "\n");
      }

      delete this.source;
    }
  }]);

  return YAMLError;
}( /*#__PURE__*/_wrapNativeSuper(Error));
export var YAMLReferenceError = /*#__PURE__*/function (_YAMLError) {
  _inherits(YAMLReferenceError, _YAMLError);

  function YAMLReferenceError(source, message) {
    _classCallCheck(this, YAMLReferenceError);

    return _possibleConstructorReturn(this, _getPrototypeOf(YAMLReferenceError).call(this, 'YAMLReferenceError', source, message));
  }

  return YAMLReferenceError;
}(YAMLError);
export var YAMLSemanticError = /*#__PURE__*/function (_YAMLError2) {
  _inherits(YAMLSemanticError, _YAMLError2);

  function YAMLSemanticError(source, message) {
    _classCallCheck(this, YAMLSemanticError);

    return _possibleConstructorReturn(this, _getPrototypeOf(YAMLSemanticError).call(this, 'YAMLSemanticError', source, message));
  }

  return YAMLSemanticError;
}(YAMLError);
export var YAMLSyntaxError = /*#__PURE__*/function (_YAMLError3) {
  _inherits(YAMLSyntaxError, _YAMLError3);

  function YAMLSyntaxError(source, message) {
    _classCallCheck(this, YAMLSyntaxError);

    return _possibleConstructorReturn(this, _getPrototypeOf(YAMLSyntaxError).call(this, 'YAMLSyntaxError', source, message));
  }

  return YAMLSyntaxError;
}(YAMLError);
export var YAMLWarning = /*#__PURE__*/function (_YAMLError4) {
  _inherits(YAMLWarning, _YAMLError4);

  function YAMLWarning(source, message) {
    _classCallCheck(this, YAMLWarning);

    return _possibleConstructorReturn(this, _getPrototypeOf(YAMLWarning).call(this, 'YAMLWarning', source, message));
  }

  return YAMLWarning;
}(YAMLError);