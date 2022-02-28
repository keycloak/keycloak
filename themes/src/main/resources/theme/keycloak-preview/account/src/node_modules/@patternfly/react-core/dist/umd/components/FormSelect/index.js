(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./FormSelect", "./FormSelectOption", "./FormSelectOptionGroup"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./FormSelect"), require("./FormSelectOption"), require("./FormSelectOptionGroup"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.FormSelect, global.FormSelectOption, global.FormSelectOptionGroup);
    global.undefined = mod.exports;
  }
})(this, function (exports, _FormSelect, _FormSelectOption, _FormSelectOptionGroup) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_FormSelect).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormSelect[key];
      }
    });
  });
  Object.keys(_FormSelectOption).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormSelectOption[key];
      }
    });
  });
  Object.keys(_FormSelectOptionGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormSelectOptionGroup[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map