"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _NotificationBadge = require("./NotificationBadge");

Object.keys(_NotificationBadge).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _NotificationBadge[key];
    }
  });
});
//# sourceMappingURL=index.js.map