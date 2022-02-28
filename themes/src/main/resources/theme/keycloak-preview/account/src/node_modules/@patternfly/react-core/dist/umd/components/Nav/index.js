(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Nav", "./NavList", "./NavGroup", "./NavItem", "./NavItemSeparator", "./NavExpandable", "./NavVariants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Nav"), require("./NavList"), require("./NavGroup"), require("./NavItem"), require("./NavItemSeparator"), require("./NavExpandable"), require("./NavVariants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Nav, global.NavList, global.NavGroup, global.NavItem, global.NavItemSeparator, global.NavExpandable, global.NavVariants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Nav, _NavList, _NavGroup, _NavItem, _NavItemSeparator, _NavExpandable, _NavVariants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Nav).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Nav[key];
      }
    });
  });
  Object.keys(_NavList).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavList[key];
      }
    });
  });
  Object.keys(_NavGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavGroup[key];
      }
    });
  });
  Object.keys(_NavItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavItem[key];
      }
    });
  });
  Object.keys(_NavItemSeparator).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavItemSeparator[key];
      }
    });
  });
  Object.keys(_NavExpandable).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavExpandable[key];
      }
    });
  });
  Object.keys(_NavVariants).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NavVariants[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map