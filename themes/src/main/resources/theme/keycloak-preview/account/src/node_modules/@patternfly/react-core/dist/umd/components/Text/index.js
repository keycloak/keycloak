(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./TextContent", "./Text", "./TextList", "./TextListItem"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./TextContent"), require("./Text"), require("./TextList"), require("./TextListItem"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.TextContent, global.Text, global.TextList, global.TextListItem);
    global.undefined = mod.exports;
  }
})(this, function (exports, _TextContent, _Text, _TextList, _TextListItem) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_TextContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextContent[key];
      }
    });
  });
  Object.keys(_Text).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Text[key];
      }
    });
  });
  Object.keys(_TextList).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextList[key];
      }
    });
  });
  Object.keys(_TextListItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextListItem[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map