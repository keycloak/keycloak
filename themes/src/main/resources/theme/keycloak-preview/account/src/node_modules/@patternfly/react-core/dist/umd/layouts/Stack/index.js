(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Stack", "./StackItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Stack"), require("./StackItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Stack, global.StackItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Stack, _StackItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Stack).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Stack[key];
      }
    });
  });
  Object.keys(_StackItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _StackItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map