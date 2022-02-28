(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Switch"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Switch"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Switch);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Switch) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Switch).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Switch[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map