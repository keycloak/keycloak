(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./SimpleList", "./SimpleListGroup", "./SimpleListItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./SimpleList"), require("./SimpleListGroup"), require("./SimpleListItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.SimpleList, global.SimpleListGroup, global.SimpleListItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _SimpleList, _SimpleListGroup, _SimpleListItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_SimpleList).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SimpleList[key];
      }
    });
  });
  Object.keys(_SimpleListGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SimpleListGroup[key];
      }
    });
  });
  Object.keys(_SimpleListItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SimpleListItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map