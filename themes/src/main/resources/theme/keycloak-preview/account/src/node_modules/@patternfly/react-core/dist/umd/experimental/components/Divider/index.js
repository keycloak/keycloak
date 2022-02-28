(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "../../../components/Divider"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("../../../components/Divider"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Divider);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Divider) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Divider).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Divider[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map