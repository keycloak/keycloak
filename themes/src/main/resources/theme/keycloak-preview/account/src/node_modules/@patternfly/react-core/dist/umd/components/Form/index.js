(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./ActionGroup", "./Form", "./FormGroup", "./FormHelperText"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./ActionGroup"), require("./Form"), require("./FormGroup"), require("./FormHelperText"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.ActionGroup, global.Form, global.FormGroup, global.FormHelperText);
    global.undefined = mod.exports;
  }
})(this, function (exports, _ActionGroup, _Form, _FormGroup, _FormHelperText) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_ActionGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ActionGroup[key];
      }
    });
  });
  Object.keys(_Form).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Form[key];
      }
    });
  });
  Object.keys(_FormGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormGroup[key];
      }
    });
  });
  Object.keys(_FormHelperText).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormHelperText[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map