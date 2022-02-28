(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Drawer", "./DrawerActions", "./DrawerCloseButton", "./DrawerContent", "./DrawerContentBody", "./DrawerHead", "./DrawerPanelBody", "./DrawerPanelContent", "./DrawerSection"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Drawer"), require("./DrawerActions"), require("./DrawerCloseButton"), require("./DrawerContent"), require("./DrawerContentBody"), require("./DrawerHead"), require("./DrawerPanelBody"), require("./DrawerPanelContent"), require("./DrawerSection"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Drawer, global.DrawerActions, global.DrawerCloseButton, global.DrawerContent, global.DrawerContentBody, global.DrawerHead, global.DrawerPanelBody, global.DrawerPanelContent, global.DrawerSection);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Drawer, _DrawerActions, _DrawerCloseButton, _DrawerContent, _DrawerContentBody, _DrawerHead, _DrawerPanelBody, _DrawerPanelContent, _DrawerSection) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Drawer).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Drawer[key];
      }
    });
  });
  Object.keys(_DrawerActions).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerActions[key];
      }
    });
  });
  Object.keys(_DrawerCloseButton).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerCloseButton[key];
      }
    });
  });
  Object.keys(_DrawerContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerContent[key];
      }
    });
  });
  Object.keys(_DrawerContentBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerContentBody[key];
      }
    });
  });
  Object.keys(_DrawerHead).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerHead[key];
      }
    });
  });
  Object.keys(_DrawerPanelBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerPanelBody[key];
      }
    });
  });
  Object.keys(_DrawerPanelContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerPanelContent[key];
      }
    });
  });
  Object.keys(_DrawerSection).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DrawerSection[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map