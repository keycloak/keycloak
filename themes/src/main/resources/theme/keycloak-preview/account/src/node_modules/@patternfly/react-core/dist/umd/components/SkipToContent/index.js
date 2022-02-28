(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./SkipToContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./SkipToContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.SkipToContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _SkipToContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_SkipToContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SkipToContent[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map