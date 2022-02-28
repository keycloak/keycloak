(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Flex", "./FlexItem", "./FlexUtils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Flex"), require("./FlexItem"), require("./FlexUtils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Flex, global.FlexItem, global.FlexUtils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Flex, _FlexItem, _FlexUtils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Flex).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Flex[key];
      }
    });
  });
  Object.keys(_FlexItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FlexItem[key];
      }
    });
  });
  Object.defineProperty(exports, "FlexBreakpoints", {
    enumerable: true,
    get: function () {
      return _FlexUtils.FlexBreakpoints;
    }
  });
  Object.defineProperty(exports, "FlexModifiers", {
    enumerable: true,
    get: function () {
      return _FlexUtils.FlexModifiers;
    }
  });
  Object.defineProperty(exports, "FlexItemModifiers", {
    enumerable: true,
    get: function () {
      return _FlexUtils.FlexItemModifiers;
    }
  });
});
//# sourceMappingURL=index.js.map