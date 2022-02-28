(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "../../../components/OverflowMenu"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("../../../components/OverflowMenu"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.OverflowMenu);
    global.undefined = mod.exports;
  }
})(this, function (exports, _OverflowMenu) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_OverflowMenu).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenu[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map