(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Title", "../../styles/sizes"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Title"), require("../../styles/sizes"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Title, global.sizes);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Title, _sizes) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Title).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Title[key];
      }
    });
  });
  Object.defineProperty(exports, "TitleSize", {
    enumerable: true,
    get: function () {
      return _sizes.BaseSizes;
    }
  });
});
//# sourceMappingURL=index.js.map