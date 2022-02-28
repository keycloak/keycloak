(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./OptionsMenu", "./OptionsMenuToggle", "./OptionsMenuItemGroup", "./OptionsMenuItem", "./OptionsMenuSeparator", "./OptionsMenuToggleWithText"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./OptionsMenu"), require("./OptionsMenuToggle"), require("./OptionsMenuItemGroup"), require("./OptionsMenuItem"), require("./OptionsMenuSeparator"), require("./OptionsMenuToggleWithText"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.OptionsMenu, global.OptionsMenuToggle, global.OptionsMenuItemGroup, global.OptionsMenuItem, global.OptionsMenuSeparator, global.OptionsMenuToggleWithText);
    global.undefined = mod.exports;
  }
})(this, function (exports, _OptionsMenu, _OptionsMenuToggle, _OptionsMenuItemGroup, _OptionsMenuItem, _OptionsMenuSeparator, _OptionsMenuToggleWithText) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_OptionsMenu).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenu[key];
      }
    });
  });
  Object.keys(_OptionsMenuToggle).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenuToggle[key];
      }
    });
  });
  Object.keys(_OptionsMenuItemGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenuItemGroup[key];
      }
    });
  });
  Object.keys(_OptionsMenuItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenuItem[key];
      }
    });
  });
  Object.keys(_OptionsMenuSeparator).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenuSeparator[key];
      }
    });
  });
  Object.keys(_OptionsMenuToggleWithText).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenuToggleWithText[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map