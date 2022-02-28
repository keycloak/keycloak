(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./EmptyState", "./EmptyStateBody", "./EmptyStateIcon", "./EmptyStateSecondaryActions", "./EmptyStatePrimary"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./EmptyState"), require("./EmptyStateBody"), require("./EmptyStateIcon"), require("./EmptyStateSecondaryActions"), require("./EmptyStatePrimary"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.EmptyState, global.EmptyStateBody, global.EmptyStateIcon, global.EmptyStateSecondaryActions, global.EmptyStatePrimary);
    global.undefined = mod.exports;
  }
})(this, function (exports, _EmptyState, _EmptyStateBody, _EmptyStateIcon, _EmptyStateSecondaryActions, _EmptyStatePrimary) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_EmptyState).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyState[key];
      }
    });
  });
  Object.keys(_EmptyStateBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyStateBody[key];
      }
    });
  });
  Object.keys(_EmptyStateIcon).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyStateIcon[key];
      }
    });
  });
  Object.keys(_EmptyStateSecondaryActions).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyStateSecondaryActions[key];
      }
    });
  });
  Object.keys(_EmptyStatePrimary).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyStatePrimary[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map