(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Backdrop"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Backdrop"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Backdrop);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Backdrop) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Backdrop).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Backdrop[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map