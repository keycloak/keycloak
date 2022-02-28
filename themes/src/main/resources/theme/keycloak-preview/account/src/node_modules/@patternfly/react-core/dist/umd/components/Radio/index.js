(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Radio"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Radio"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Radio);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Radio) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Radio).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Radio[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map