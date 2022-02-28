(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Split", "./SplitItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Split"), require("./SplitItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Split, global.SplitItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Split, _SplitItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Split).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Split[key];
      }
    });
  });
  Object.keys(_SplitItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SplitItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map