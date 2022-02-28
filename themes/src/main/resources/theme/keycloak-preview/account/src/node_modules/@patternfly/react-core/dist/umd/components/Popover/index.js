(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Popover"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Popover"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Popover);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Popover) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.defineProperty(exports, "Popover", {
    enumerable: true,
    get: function () {
      return _Popover.Popover;
    }
  });
  Object.defineProperty(exports, "PopoverPosition", {
    enumerable: true,
    get: function () {
      return _Popover.PopoverPosition;
    }
  });
});
//# sourceMappingURL=index.js.map