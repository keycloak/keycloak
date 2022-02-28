(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Level", "./LevelItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Level"), require("./LevelItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Level, global.LevelItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Level, _LevelItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Level).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Level[key];
      }
    });
  });
  Object.keys(_LevelItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LevelItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map