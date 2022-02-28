(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports);
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports);
    global.undefined = mod.exports;
  }
})(this, function (exports) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  let BaseSizes = exports.BaseSizes = undefined;

  (function (BaseSizes) {
    BaseSizes["xs"] = "xs";
    BaseSizes["sm"] = "sm";
    BaseSizes["md"] = "md";
    BaseSizes["lg"] = "lg";
    BaseSizes["xl"] = "xl";
    BaseSizes["2xl"] = "2xl";
    BaseSizes["3xl"] = "3xl";
    BaseSizes["4xl"] = "4xl";
  })(BaseSizes || (exports.BaseSizes = BaseSizes = {}));

  let DeviceSizes = exports.DeviceSizes = undefined;

  (function (DeviceSizes) {
    DeviceSizes["sm"] = "Sm";
    DeviceSizes["md"] = "Md";
    DeviceSizes["lg"] = "Lg";
    DeviceSizes["xl"] = "Xl";
    DeviceSizes["xl2"] = "_2xl";
  })(DeviceSizes || (exports.DeviceSizes = DeviceSizes = {}));
});
//# sourceMappingURL=sizes.js.map