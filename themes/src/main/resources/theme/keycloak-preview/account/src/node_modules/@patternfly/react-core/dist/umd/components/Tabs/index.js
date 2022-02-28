(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Tab", "./Tabs", "./TabContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Tab"), require("./Tabs"), require("./TabContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Tab, global.Tabs, global.TabContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Tab, _Tabs, _TabContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Tab).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Tab[key];
      }
    });
  });
  Object.keys(_Tabs).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Tabs[key];
      }
    });
  });
  Object.keys(_TabContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TabContent[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map