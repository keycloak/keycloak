(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./TextArea"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./TextArea"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.TextArea);
    global.undefined = mod.exports;
  }
})(this, function (exports, _TextArea) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_TextArea).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextArea[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map