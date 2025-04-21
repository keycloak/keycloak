"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.proposalSyntaxPlugins = exports.proposalPlugins = exports.pluginSyntaxMap = void 0;
const proposalPlugins = new Set();
exports.proposalPlugins = proposalPlugins;
const proposalSyntaxPlugins = ["syntax-import-assertions"];
exports.proposalSyntaxPlugins = proposalSyntaxPlugins;
const pluginSyntaxObject = {
  "proposal-async-generator-functions": "syntax-async-generators",
  "proposal-class-properties": "syntax-class-properties",
  "proposal-class-static-block": "syntax-class-static-block",
  "proposal-json-strings": "syntax-json-strings",
  "proposal-nullish-coalescing-operator": "syntax-nullish-coalescing-operator",
  "proposal-numeric-separator": "syntax-numeric-separator",
  "proposal-object-rest-spread": "syntax-object-rest-spread",
  "proposal-optional-catch-binding": "syntax-optional-catch-binding",
  "proposal-optional-chaining": "syntax-optional-chaining",
  "proposal-private-methods": "syntax-class-properties",
  "proposal-private-property-in-object": "syntax-private-property-in-object",
  "proposal-unicode-property-regex": null
};
const pluginSyntaxEntries = Object.keys(pluginSyntaxObject).map(function (key) {
  return [key, pluginSyntaxObject[key]];
});
const pluginSyntaxMap = new Map(pluginSyntaxEntries);
exports.pluginSyntaxMap = pluginSyntaxMap;