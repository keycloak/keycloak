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
  let NavVariants = exports.NavVariants = undefined;

  (function (NavVariants) {
    NavVariants["default"] = "default";
    NavVariants["simple"] = "simple";
    NavVariants["horizontal"] = "horizontal";
    NavVariants["tertiary"] = "tertiary";
  })(NavVariants || (exports.NavVariants = NavVariants = {}));
});
//# sourceMappingURL=NavVariants.js.map