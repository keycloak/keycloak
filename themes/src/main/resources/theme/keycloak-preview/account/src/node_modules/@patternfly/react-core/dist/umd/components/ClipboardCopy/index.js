(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./ClipboardCopy"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./ClipboardCopy"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.ClipboardCopy);
    global.undefined = mod.exports;
  }
})(this, function (exports, _ClipboardCopy) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_ClipboardCopy).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ClipboardCopy[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map