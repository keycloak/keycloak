(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Page", "./PageHeader", "./PageSidebar", "./PageSection"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Page"), require("./PageHeader"), require("./PageSidebar"), require("./PageSection"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Page, global.PageHeader, global.PageSidebar, global.PageSection);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Page, _PageHeader, _PageSidebar, _PageSection) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Page).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Page[key];
      }
    });
  });
  Object.keys(_PageHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _PageHeader[key];
      }
    });
  });
  Object.keys(_PageSidebar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _PageSidebar[key];
      }
    });
  });
  Object.keys(_PageSection).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _PageSection[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map