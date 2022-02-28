(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./List", "./ListItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./List"), require("./ListItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.List, global.ListItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _List, _ListItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_List).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _List[key];
      }
    });
  });
  Object.keys(_ListItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ListItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map