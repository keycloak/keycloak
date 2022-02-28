(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./OverflowMenu", "./OverflowMenuControl", "./OverflowMenuContent", "./OverflowMenuGroup", "./OverflowMenuItem", "./OverflowMenuDropdownItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./OverflowMenu"), require("./OverflowMenuControl"), require("./OverflowMenuContent"), require("./OverflowMenuGroup"), require("./OverflowMenuItem"), require("./OverflowMenuDropdownItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.OverflowMenu, global.OverflowMenuControl, global.OverflowMenuContent, global.OverflowMenuGroup, global.OverflowMenuItem, global.OverflowMenuDropdownItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _OverflowMenu, _OverflowMenuControl, _OverflowMenuContent, _OverflowMenuGroup, _OverflowMenuItem, _OverflowMenuDropdownItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_OverflowMenu).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenu[key];
      }
    });
  });
  Object.keys(_OverflowMenuControl).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenuControl[key];
      }
    });
  });
  Object.keys(_OverflowMenuContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenuContent[key];
      }
    });
  });
  Object.keys(_OverflowMenuGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenuGroup[key];
      }
    });
  });
  Object.keys(_OverflowMenuItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenuItem[key];
      }
    });
  });
  Object.keys(_OverflowMenuDropdownItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OverflowMenuDropdownItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map