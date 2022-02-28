(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Pagination", "./ToggleTemplate"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Pagination"), require("./ToggleTemplate"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Pagination, global.ToggleTemplate);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Pagination, _ToggleTemplate) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Pagination).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Pagination[key];
      }
    });
  });
  Object.keys(_ToggleTemplate).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ToggleTemplate[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map