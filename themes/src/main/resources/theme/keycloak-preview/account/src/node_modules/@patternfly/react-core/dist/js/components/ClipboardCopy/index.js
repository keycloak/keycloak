"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _ClipboardCopy = require("./ClipboardCopy");

Object.keys(_ClipboardCopy).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _ClipboardCopy[key];
    }
  });
});
//# sourceMappingURL=index.js.map