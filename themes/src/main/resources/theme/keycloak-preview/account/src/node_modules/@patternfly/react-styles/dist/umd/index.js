(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./StyleSheet", "./utils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./StyleSheet"), require("./utils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.StyleSheet, global.utils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _StyleSheet, _utils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.defineProperty(exports, "StyleSheet", {
    enumerable: true,
    get: function () {
      return _StyleSheet.StyleSheet;
    }
  });
  Object.defineProperty(exports, "css", {
    enumerable: true,
    get: function () {
      return _StyleSheet.css;
    }
  });
  Object.defineProperty(exports, "isValidStyleDeclaration", {
    enumerable: true,
    get: function () {
      return _utils.isValidStyleDeclaration;
    }
  });
  Object.defineProperty(exports, "getModifier", {
    enumerable: true,
    get: function () {
      return _utils.getModifier;
    }
  });
  Object.defineProperty(exports, "isModifier", {
    enumerable: true,
    get: function () {
      return _utils.isModifier;
    }
  });
  Object.defineProperty(exports, "getInsertedStyles", {
    enumerable: true,
    get: function () {
      return _utils.getInsertedStyles;
    }
  });
  Object.defineProperty(exports, "getClassName", {
    enumerable: true,
    get: function () {
      return _utils.getClassName;
    }
  });
  Object.defineProperty(exports, "pickProperties", {
    enumerable: true,
    get: function () {
      return _utils.pickProperties;
    }
  });
});
//# sourceMappingURL=index.js.map