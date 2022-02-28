(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Progress", "./ProgressBar", "./ProgressContainer"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Progress"), require("./ProgressBar"), require("./ProgressContainer"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Progress, global.ProgressBar, global.ProgressContainer);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Progress, _ProgressBar, _ProgressContainer) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Progress).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Progress[key];
      }
    });
  });
  Object.keys(_ProgressBar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ProgressBar[key];
      }
    });
  });
  Object.keys(_ProgressContainer).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ProgressContainer[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map