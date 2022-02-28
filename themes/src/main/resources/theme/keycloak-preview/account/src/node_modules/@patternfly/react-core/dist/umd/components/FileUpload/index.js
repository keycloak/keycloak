(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./FileUploadField", "./FileUpload"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./FileUploadField"), require("./FileUpload"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.FileUploadField, global.FileUpload);
    global.undefined = mod.exports;
  }
})(this, function (exports, _FileUploadField, _FileUpload) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_FileUploadField).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FileUploadField[key];
      }
    });
  });
  Object.keys(_FileUpload).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FileUpload[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map