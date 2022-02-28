(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Avatar"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Avatar"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Avatar);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Avatar) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Avatar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Avatar[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map