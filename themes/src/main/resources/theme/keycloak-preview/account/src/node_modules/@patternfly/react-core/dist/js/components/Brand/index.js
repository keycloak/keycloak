"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _Brand = require("./Brand");

Object.keys(_Brand).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Brand[key];
    }
  });
});
//# sourceMappingURL=index.js.map