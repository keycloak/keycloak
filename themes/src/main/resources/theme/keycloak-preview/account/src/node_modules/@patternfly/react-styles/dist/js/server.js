"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.renderStatic = renderStatic;

var _emotionServer = require("emotion-server");

/**
 * @param {Function} renderFn - Render function
 */
function renderStatic(renderFn) {
  var html = (0, _emotionServer.renderStylesToString)(renderFn());
  return {
    html: html
  };
}
//# sourceMappingURL=server.js.map