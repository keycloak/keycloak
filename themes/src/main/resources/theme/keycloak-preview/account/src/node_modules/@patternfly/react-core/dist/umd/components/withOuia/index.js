(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_withOuia).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _withOuia[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map