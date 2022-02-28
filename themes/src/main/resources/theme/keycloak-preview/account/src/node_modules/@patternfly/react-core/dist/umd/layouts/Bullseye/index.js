(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Bullseye"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Bullseye"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Bullseye);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Bullseye) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Bullseye).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Bullseye[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map