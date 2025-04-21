"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _helperPluginUtils = require("@babel/helper-plugin-utils");

var _pluginSyntaxOptionalCatchBinding = require("@babel/plugin-syntax-optional-catch-binding");

var _default = (0, _helperPluginUtils.declare)(api => {
  api.assertVersion(7);
  return {
    name: "proposal-optional-catch-binding",
    inherits: _pluginSyntaxOptionalCatchBinding.default,
    visitor: {
      CatchClause(path) {
        if (!path.node.param) {
          const uid = path.scope.generateUidIdentifier("unused");
          const paramPath = path.get("param");
          paramPath.replaceWith(uid);
        }
      }

    }
  };
});

exports.default = _default;