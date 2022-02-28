(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./NotificationBadge"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./NotificationBadge"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.NotificationBadge);
    global.undefined = mod.exports;
  }
})(this, function (exports, _NotificationBadge) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_NotificationBadge).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NotificationBadge[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map