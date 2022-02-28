(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Grid", "./GridItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Grid"), require("./GridItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Grid, global.GridItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Grid, _GridItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Grid).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Grid[key];
      }
    });
  });
  Object.keys(_GridItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _GridItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map