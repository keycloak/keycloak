(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Spinner"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Spinner"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Spinner);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Spinner) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Spinner).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Spinner[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map