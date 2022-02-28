(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Modal", "./ModalBox", "./ModalBoxBody", "./ModalBoxCloseButton", "./ModalBoxFooter", "./ModalBoxHeader", "./ModalContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Modal"), require("./ModalBox"), require("./ModalBoxBody"), require("./ModalBoxCloseButton"), require("./ModalBoxFooter"), require("./ModalBoxHeader"), require("./ModalContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Modal, global.ModalBox, global.ModalBoxBody, global.ModalBoxCloseButton, global.ModalBoxFooter, global.ModalBoxHeader, global.ModalContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Modal, _ModalBox, _ModalBoxBody, _ModalBoxCloseButton, _ModalBoxFooter, _ModalBoxHeader, _ModalContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Modal).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Modal[key];
      }
    });
  });
  Object.keys(_ModalBox).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalBox[key];
      }
    });
  });
  Object.keys(_ModalBoxBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalBoxBody[key];
      }
    });
  });
  Object.keys(_ModalBoxCloseButton).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalBoxCloseButton[key];
      }
    });
  });
  Object.keys(_ModalBoxFooter).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalBoxFooter[key];
      }
    });
  });
  Object.keys(_ModalBoxHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalBoxHeader[key];
      }
    });
  });
  Object.keys(_ModalContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ModalContent[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map