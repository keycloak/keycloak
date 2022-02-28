(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./AlertGroup"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./AlertGroup"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.AlertGroup);
    global.undefined = mod.exports;
  }
})(this, function (exports, _AlertGroup) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.defineProperty(exports, "AlertGroup", {
    enumerable: true,
    get: function () {
      return _AlertGroup.AlertGroup;
    }
  });
});
//# sourceMappingURL=index.js.map