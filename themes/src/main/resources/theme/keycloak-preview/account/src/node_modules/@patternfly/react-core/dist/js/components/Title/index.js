"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
var _exportNames = {
  TitleSize: true
};
Object.defineProperty(exports, "TitleSize", {
  enumerable: true,
  get: function get() {
    return _sizes.BaseSizes;
  }
});

var _Title = require("./Title");

Object.keys(_Title).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (Object.prototype.hasOwnProperty.call(_exportNames, key)) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Title[key];
    }
  });
});

var _sizes = require("../../styles/sizes");
//# sourceMappingURL=index.js.map