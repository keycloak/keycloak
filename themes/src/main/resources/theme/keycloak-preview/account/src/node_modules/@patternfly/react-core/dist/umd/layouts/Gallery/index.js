(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Gallery", "./GalleryItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Gallery"), require("./GalleryItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Gallery, global.GalleryItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Gallery, _GalleryItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Gallery).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Gallery[key];
      }
    });
  });
  Object.keys(_GalleryItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _GalleryItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map