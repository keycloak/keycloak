(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Brand"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Brand"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Brand);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Brand) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Brand).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Brand[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map