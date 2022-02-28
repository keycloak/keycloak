(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Card", "./CardActions", "./CardBody", "./CardFooter", "./CardHeader", "./CardHead", "./CardHeadMain"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Card"), require("./CardActions"), require("./CardBody"), require("./CardFooter"), require("./CardHeader"), require("./CardHead"), require("./CardHeadMain"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Card, global.CardActions, global.CardBody, global.CardFooter, global.CardHeader, global.CardHead, global.CardHeadMain);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Card, _CardActions, _CardBody, _CardFooter, _CardHeader, _CardHead, _CardHeadMain) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Card).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Card[key];
      }
    });
  });
  Object.keys(_CardActions).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardActions[key];
      }
    });
  });
  Object.keys(_CardBody).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardBody[key];
      }
    });
  });
  Object.keys(_CardFooter).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardFooter[key];
      }
    });
  });
  Object.keys(_CardHeader).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardHeader[key];
      }
    });
  });
  Object.keys(_CardHead).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardHead[key];
      }
    });
  });
  Object.keys(_CardHeadMain).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _CardHeadMain[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map