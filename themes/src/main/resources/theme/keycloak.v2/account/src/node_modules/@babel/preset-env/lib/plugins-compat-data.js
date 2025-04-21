"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.pluginsBugfixes = exports.plugins = void 0;

var _plugins = require("@babel/compat-data/plugins");

var _pluginBugfixes = require("@babel/compat-data/plugin-bugfixes");

var _availablePlugins = require("./available-plugins");

const pluginsFiltered = {};
exports.plugins = pluginsFiltered;
const bugfixPluginsFiltered = {};
exports.pluginsBugfixes = bugfixPluginsFiltered;

for (const plugin of Object.keys(_plugins)) {
  if (Object.hasOwnProperty.call(_availablePlugins.default, plugin)) {
    pluginsFiltered[plugin] = _plugins[plugin];
  }
}

for (const plugin of Object.keys(_pluginBugfixes)) {
  if (Object.hasOwnProperty.call(_availablePlugins.default, plugin)) {
    bugfixPluginsFiltered[plugin] = _pluginBugfixes[plugin];
  }
}