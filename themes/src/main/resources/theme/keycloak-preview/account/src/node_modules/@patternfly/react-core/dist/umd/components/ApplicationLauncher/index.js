(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./ApplicationLauncher", "./ApplicationLauncherContext", "./ApplicationLauncherItem", "./ApplicationLauncherItemContext", "./ApplicationLauncherContent", "./ApplicationLauncherIcon", "./ApplicationLauncherText", "./ApplicationLauncherGroup", "./ApplicationLauncherSeparator"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./ApplicationLauncher"), require("./ApplicationLauncherContext"), require("./ApplicationLauncherItem"), require("./ApplicationLauncherItemContext"), require("./ApplicationLauncherContent"), require("./ApplicationLauncherIcon"), require("./ApplicationLauncherText"), require("./ApplicationLauncherGroup"), require("./ApplicationLauncherSeparator"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.ApplicationLauncher, global.ApplicationLauncherContext, global.ApplicationLauncherItem, global.ApplicationLauncherItemContext, global.ApplicationLauncherContent, global.ApplicationLauncherIcon, global.ApplicationLauncherText, global.ApplicationLauncherGroup, global.ApplicationLauncherSeparator);
    global.undefined = mod.exports;
  }
})(this, function (exports, _ApplicationLauncher, _ApplicationLauncherContext, _ApplicationLauncherItem, _ApplicationLauncherItemContext, _ApplicationLauncherContent, _ApplicationLauncherIcon, _ApplicationLauncherText, _ApplicationLauncherGroup, _ApplicationLauncherSeparator) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_ApplicationLauncher).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncher[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherContext).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherContext[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherItem[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherItemContext).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherItemContext[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherContent[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherIcon).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherIcon[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherText).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherText[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherGroup[key];
      }
    });
  });
  Object.keys(_ApplicationLauncherSeparator).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncherSeparator[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map