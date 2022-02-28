(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Breadcrumb", "./BreadcrumbItem", "./BreadcrumbHeading"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Breadcrumb"), require("./BreadcrumbItem"), require("./BreadcrumbHeading"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Breadcrumb, global.BreadcrumbItem, global.BreadcrumbHeading);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Breadcrumb, _BreadcrumbItem, _BreadcrumbHeading) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Breadcrumb).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Breadcrumb[key];
      }
    });
  });
  Object.keys(_BreadcrumbItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _BreadcrumbItem[key];
      }
    });
  });
  Object.keys(_BreadcrumbHeading).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _BreadcrumbHeading[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map