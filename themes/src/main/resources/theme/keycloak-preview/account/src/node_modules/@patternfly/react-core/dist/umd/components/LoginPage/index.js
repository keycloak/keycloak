(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./LoginPage", "./Login", "./LoginForm", "./LoginMainBody", "./LoginMainHeader", "./LoginHeader", "./LoginFooter", "./LoginMainFooter", "./LoginFooterItem", "./LoginMainFooterBandItem", "./LoginMainFooterLinksItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./LoginPage"), require("./Login"), require("./LoginForm"), require("./LoginMainBody"), require("./LoginMainHeader"), require("./LoginHeader"), require("./LoginFooter"), require("./LoginMainFooter"), require("./LoginFooterItem"), require("./LoginMainFooterBandItem"), require("./LoginMainFooterLinksItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.LoginPage, global.Login, global.LoginForm, global.LoginMainBody, global.LoginMainHeader, global.LoginHeader, global.LoginFooter, global.LoginMainFooter, global.LoginFooterItem, global.LoginMainFooterBandItem, global.LoginMainFooterLinksItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _LoginPage, _Login, _LoginForm, _LoginMainBody, _LoginMainHeader, _LoginHeader, _LoginFooter, _LoginMainFooter, _LoginFooterItem, _LoginMainFooterBandItem, _LoginMainFooterLinksItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_LoginPage).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginPage[key];
      }
    });
  });
  Object.keys(_Login).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Login[key];
      }
    });
  });
  Object.keys(_LoginForm).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginForm[key];
      }
    });
  });
  Object.keys(_LoginMainBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginMainBody[key];
      }
    });
  });
  Object.keys(_LoginMainHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginMainHeader[key];
      }
    });
  });
  Object.keys(_LoginHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginHeader[key];
      }
    });
  });
  Object.keys(_LoginFooter).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginFooter[key];
      }
    });
  });
  Object.keys(_LoginMainFooter).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginMainFooter[key];
      }
    });
  });
  Object.keys(_LoginFooterItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginFooterItem[key];
      }
    });
  });
  Object.keys(_LoginMainFooterBandItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginMainFooterBandItem[key];
      }
    });
  });
  Object.keys(_LoginMainFooterLinksItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginMainFooterLinksItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map