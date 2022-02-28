(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports);
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports);
    global.undefined = mod.exports;
  }
})(this, function (exports) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  const KEY_CODES = exports.KEY_CODES = {
    ARROW_UP: 38,
    ARROW_DOWN: 40,
    ESCAPE_KEY: 27,
    TAB: 9,
    ENTER: 13,
    SPACE: 32
  };
  const SIDE = exports.SIDE = {
    RIGHT: 'right',
    LEFT: 'left',
    BOTH: 'both',
    NONE: 'none'
  };
  const KEYHANDLER_DIRECTION = exports.KEYHANDLER_DIRECTION = {
    UP: 'up',
    DOWN: 'down',
    RIGHT: 'right',
    LEFT: 'left'
  };
  let ValidatedOptions = exports.ValidatedOptions = undefined;

  (function (ValidatedOptions) {
    ValidatedOptions["success"] = "success";
    ValidatedOptions["error"] = "error";
    ValidatedOptions["default"] = "default";
  })(ValidatedOptions || (exports.ValidatedOptions = ValidatedOptions = {}));
});
//# sourceMappingURL=constants.js.map