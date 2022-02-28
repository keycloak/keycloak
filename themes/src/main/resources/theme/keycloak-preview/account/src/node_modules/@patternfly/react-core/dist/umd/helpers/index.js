(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./constants", "./FocusTrap/FocusTrap", "./GenerateId/GenerateId", "./htmlConstants", "./typeUtils", "./util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./constants"), require("./FocusTrap/FocusTrap"), require("./GenerateId/GenerateId"), require("./htmlConstants"), require("./typeUtils"), require("./util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.constants, global.FocusTrap, global.GenerateId, global.htmlConstants, global.typeUtils, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _constants, _FocusTrap, _GenerateId, _htmlConstants, _typeUtils, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_constants).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _constants[key];
      }
    });
  });
  Object.keys(_FocusTrap).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FocusTrap[key];
      }
    });
  });
  Object.defineProperty(exports, "GenerateId", {
    enumerable: true,
    get: function () {
      return _interopRequireDefault(_GenerateId).default;
    }
  });
  Object.keys(_htmlConstants).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _htmlConstants[key];
      }
    });
  });
  Object.keys(_typeUtils).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _typeUtils[key];
      }
    });
  });
  Object.keys(_util).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _util[key];
      }
    });
  });

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }
});
//# sourceMappingURL=index.js.map