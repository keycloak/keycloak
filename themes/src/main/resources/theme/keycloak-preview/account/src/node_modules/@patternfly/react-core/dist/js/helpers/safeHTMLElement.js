"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _util = require("./util");

// https://github.com/reactjs/react-modal/blob/master/src/helpers/safeHTMLElement.js
var _default = _util.canUseDOM ? window.HTMLElement : {};

exports["default"] = _default;
//# sourceMappingURL=safeHTMLElement.js.map