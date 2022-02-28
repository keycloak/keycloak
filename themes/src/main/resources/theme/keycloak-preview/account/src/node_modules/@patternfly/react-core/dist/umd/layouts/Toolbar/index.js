(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Toolbar", "./ToolbarItem", "./ToolbarGroup", "./ToolbarSection"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Toolbar"), require("./ToolbarItem"), require("./ToolbarGroup"), require("./ToolbarSection"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Toolbar, global.ToolbarItem, global.ToolbarGroup, global.ToolbarSection);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Toolbar, _ToolbarItem, _ToolbarGroup, _ToolbarSection) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Toolbar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Toolbar[key];
      }
    });
  });
  Object.keys(_ToolbarItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ToolbarItem[key];
      }
    });
  });
  Object.keys(_ToolbarGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ToolbarGroup[key];
      }
    });
  });
  Object.keys(_ToolbarSection).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ToolbarSection[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map