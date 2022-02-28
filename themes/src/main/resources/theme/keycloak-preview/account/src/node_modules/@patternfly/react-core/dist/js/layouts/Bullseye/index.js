"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _Bullseye = require("./Bullseye");

Object.keys(_Bullseye).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Bullseye[key];
    }
  });
});
//# sourceMappingURL=index.js.map