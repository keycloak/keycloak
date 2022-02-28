(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./ChipGroup", "./ChipGroupToolbarItem", "./Chip"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./ChipGroup"), require("./ChipGroupToolbarItem"), require("./Chip"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.ChipGroup, global.ChipGroupToolbarItem, global.Chip);
    global.undefined = mod.exports;
  }
})(this, function (exports, _ChipGroup, _ChipGroupToolbarItem, _Chip) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_ChipGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ChipGroup[key];
      }
    });
  });
  Object.keys(_ChipGroupToolbarItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ChipGroupToolbarItem[key];
      }
    });
  });
  Object.keys(_Chip).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Chip[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map