"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ValidatedOptions = exports.KEYHANDLER_DIRECTION = exports.SIDE = exports.KEY_CODES = void 0;
var KEY_CODES = {
  ARROW_UP: 38,
  ARROW_DOWN: 40,
  ESCAPE_KEY: 27,
  TAB: 9,
  ENTER: 13,
  SPACE: 32
};
exports.KEY_CODES = KEY_CODES;
var SIDE = {
  RIGHT: 'right',
  LEFT: 'left',
  BOTH: 'both',
  NONE: 'none'
};
exports.SIDE = SIDE;
var KEYHANDLER_DIRECTION = {
  UP: 'up',
  DOWN: 'down',
  RIGHT: 'right',
  LEFT: 'left'
};
exports.KEYHANDLER_DIRECTION = KEYHANDLER_DIRECTION;
var ValidatedOptions;
exports.ValidatedOptions = ValidatedOptions;

(function (ValidatedOptions) {
  ValidatedOptions["success"] = "success";
  ValidatedOptions["error"] = "error";
  ValidatedOptions["default"] = "default";
})(ValidatedOptions || (exports.ValidatedOptions = ValidatedOptions = {}));
//# sourceMappingURL=constants.js.map