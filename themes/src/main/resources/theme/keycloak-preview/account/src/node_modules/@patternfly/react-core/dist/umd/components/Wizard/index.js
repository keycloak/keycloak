(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Wizard", "./WizardContext", "./WizardNav", "./WizardNavItem", "./WizardHeader", "./WizardBody", "./WizardFooter", "./WizardToggle"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Wizard"), require("./WizardContext"), require("./WizardNav"), require("./WizardNavItem"), require("./WizardHeader"), require("./WizardBody"), require("./WizardFooter"), require("./WizardToggle"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Wizard, global.WizardContext, global.WizardNav, global.WizardNavItem, global.WizardHeader, global.WizardBody, global.WizardFooter, global.WizardToggle);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Wizard, _WizardContext, _WizardNav, _WizardNavItem, _WizardHeader, _WizardBody, _WizardFooter, _WizardToggle) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Wizard).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Wizard[key];
      }
    });
  });
  Object.keys(_WizardContext).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardContext[key];
      }
    });
  });
  Object.keys(_WizardNav).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardNav[key];
      }
    });
  });
  Object.keys(_WizardNavItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardNavItem[key];
      }
    });
  });
  Object.keys(_WizardHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardHeader[key];
      }
    });
  });
  Object.keys(_WizardBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardBody[key];
      }
    });
  });
  Object.keys(_WizardFooter).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardFooter[key];
      }
    });
  });
  Object.keys(_WizardToggle).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _WizardToggle[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map