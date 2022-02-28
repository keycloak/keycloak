"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _Spinner = require("../../../components/Spinner");

Object.keys(_Spinner).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Spinner[key];
    }
  });
});
//# sourceMappingURL=index.js.map