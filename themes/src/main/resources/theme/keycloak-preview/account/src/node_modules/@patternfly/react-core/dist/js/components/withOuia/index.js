"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _withOuia = require("./withOuia");

Object.keys(_withOuia).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _withOuia[key];
    }
  });
});
//# sourceMappingURL=index.js.map