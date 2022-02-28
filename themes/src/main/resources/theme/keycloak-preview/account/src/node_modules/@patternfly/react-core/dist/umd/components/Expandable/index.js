(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Expandable"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Expandable"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Expandable);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Expandable) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Expandable).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Expandable[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map