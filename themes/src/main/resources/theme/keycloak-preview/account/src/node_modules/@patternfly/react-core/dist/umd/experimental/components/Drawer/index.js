(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "../../../components/Drawer"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("../../../components/Drawer"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Drawer);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Drawer) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Drawer).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Drawer[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map