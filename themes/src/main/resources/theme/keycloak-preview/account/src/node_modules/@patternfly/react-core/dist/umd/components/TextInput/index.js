(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./TextInput"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./TextInput"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.TextInput);
    global.undefined = mod.exports;
  }
})(this, function (exports, _TextInput) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_TextInput).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextInput[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map