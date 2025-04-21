"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _helperPluginUtils = require("@babel/helper-plugin-utils");

var _default = (0, _helperPluginUtils.declare)(api => {
  api.assertVersion(7);
  return {
    name: "syntax-jsx",

    manipulateOptions(opts, parserOpts) {
      const {
        plugins
      } = parserOpts;

      if (plugins.some(p => (Array.isArray(p) ? p[0] : p) === "typescript")) {
        return;
      }

      plugins.push("jsx");
    }

  };
});

exports.default = _default;