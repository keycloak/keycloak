(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "emotion-server"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("emotion-server"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.emotionServer);
    global.undefined = mod.exports;
  }
})(this, function (exports, _emotionServer) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.renderStatic = renderStatic;

  /**
   * @param {Function} renderFn - Render function
   */
  function renderStatic(renderFn) {
    const html = (0, _emotionServer.renderStylesToString)(renderFn());
    return {
      html
    };
  }
});
//# sourceMappingURL=server.js.map