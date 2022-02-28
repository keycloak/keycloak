(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./ContextSelector", "./ContextSelectorItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./ContextSelector"), require("./ContextSelectorItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.ContextSelector, global.ContextSelectorItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _ContextSelector, _ContextSelectorItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_ContextSelector).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ContextSelector[key];
      }
    });
  });
  Object.keys(_ContextSelectorItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ContextSelectorItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map