"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _Gallery = require("./Gallery");

Object.keys(_Gallery).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Gallery[key];
    }
  });
});

var _GalleryItem = require("./GalleryItem");

Object.keys(_GalleryItem).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _GalleryItem[key];
    }
  });
});
//# sourceMappingURL=index.js.map