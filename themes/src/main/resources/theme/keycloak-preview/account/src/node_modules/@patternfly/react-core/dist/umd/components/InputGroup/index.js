(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./InputGroup", "./InputGroupText"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./InputGroup"), require("./InputGroupText"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.InputGroup, global.InputGroupText);
    global.undefined = mod.exports;
  }
})(this, function (exports, _InputGroup, _InputGroupText) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_InputGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _InputGroup[key];
      }
    });
  });
  Object.keys(_InputGroupText).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _InputGroupText[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map