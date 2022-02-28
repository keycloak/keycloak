"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _helperPluginUtils = require("@babel/helper-plugin-utils");

var _pluginTransformTypescript = _interopRequireDefault(require("@babel/plugin-transform-typescript"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var _default = (0, _helperPluginUtils.declare)((api, {
  jsxPragma,
  allExtensions = false,
  isTSX = false,
  allowNamespaces,
  allowDeclareFields
}) => {
  api.assertVersion(7);

  if (typeof allExtensions !== "boolean") {
    throw new Error(".allExtensions must be a boolean, or undefined");
  }

  if (typeof isTSX !== "boolean") {
    throw new Error(".isTSX must be a boolean, or undefined");
  }

  if (isTSX && !allExtensions) {
    throw new Error("isTSX:true requires allExtensions:true");
  }

  const pluginOptions = isTSX => ({
    jsxPragma,
    isTSX,
    allowNamespaces,
    allowDeclareFields
  });

  return {
    overrides: allExtensions ? [{
      plugins: [[_pluginTransformTypescript.default, pluginOptions(isTSX)]]
    }] : [{
      test: /\.ts$/,
      plugins: [[_pluginTransformTypescript.default, pluginOptions(false)]]
    }, {
      test: /\.tsx$/,
      plugins: [[_pluginTransformTypescript.default, pluginOptions(true)]]
    }]
  };
});

exports.default = _default;