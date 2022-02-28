(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "../../../components/DataToolbar"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("../../../components/DataToolbar"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.DataToolbar);
    global.undefined = mod.exports;
  }
})(this, function (exports, _DataToolbar) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_DataToolbar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DataToolbar[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map