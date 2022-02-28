(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./AboutModal"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./AboutModal"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.AboutModal);
    global.undefined = mod.exports;
  }
})(this, function (exports, _AboutModal) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_AboutModal).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AboutModal[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map