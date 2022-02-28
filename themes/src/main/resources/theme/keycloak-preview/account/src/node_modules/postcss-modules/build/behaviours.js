"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.behaviours = undefined;
exports.getDefaultPlugins = getDefaultPlugins;
exports.isValidBehaviour = isValidBehaviour;

var _cssModulesLoaderCore = require("css-modules-loader-core");

var _cssModulesLoaderCore2 = _interopRequireDefault(_cssModulesLoaderCore);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const behaviours = exports.behaviours = {
  LOCAL: "local",
  GLOBAL: "global"
};

function getDefaultPlugins(behaviour, generateScopedName) {
  const scope = _cssModulesLoaderCore2.default.scope({ generateScopedName });

  const plugins = {
    [behaviours.LOCAL]: [_cssModulesLoaderCore2.default.values, _cssModulesLoaderCore2.default.localByDefault, _cssModulesLoaderCore2.default.extractImports, scope],

    [behaviours.GLOBAL]: [_cssModulesLoaderCore2.default.values, _cssModulesLoaderCore2.default.extractImports, scope]
  };

  return plugins[behaviour];
}

function isValidBehaviour(behaviour) {
  return Object.keys(behaviours).map(key => behaviours[key]).indexOf(behaviour) > -1;
}