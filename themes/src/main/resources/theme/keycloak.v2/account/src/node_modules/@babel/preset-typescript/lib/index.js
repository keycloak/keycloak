'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var helperPluginUtils = require('@babel/helper-plugin-utils');
var transformTypeScript = require('@babel/plugin-transform-typescript');
var helperValidatorOption = require('@babel/helper-validator-option');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

var transformTypeScript__default = /*#__PURE__*/_interopDefaultLegacy(transformTypeScript);

const v = new helperValidatorOption.OptionValidator("@babel/preset-typescript");
function normalizeOptions(options = {}) {
  let {
    allowNamespaces = true,
    jsxPragma,
    onlyRemoveTypeImports
  } = options;
  const TopLevelOptions = {
    allExtensions: "allExtensions",
    allowNamespaces: "allowNamespaces",
    disallowAmbiguousJSXLike: "disallowAmbiguousJSXLike",
    isTSX: "isTSX",
    jsxPragma: "jsxPragma",
    jsxPragmaFrag: "jsxPragmaFrag",
    onlyRemoveTypeImports: "onlyRemoveTypeImports",
    optimizeConstEnums: "optimizeConstEnums"
  };
  const jsxPragmaFrag = v.validateStringOption(TopLevelOptions.jsxPragmaFrag, options.jsxPragmaFrag, "React.Fragment");
  const allExtensions = v.validateBooleanOption(TopLevelOptions.allExtensions, options.allExtensions, false);
  const isTSX = v.validateBooleanOption(TopLevelOptions.isTSX, options.isTSX, false);

  if (isTSX) {
    v.invariant(allExtensions, "isTSX:true requires allExtensions:true");
  }

  const disallowAmbiguousJSXLike = v.validateBooleanOption(TopLevelOptions.disallowAmbiguousJSXLike, options.disallowAmbiguousJSXLike, false);

  if (disallowAmbiguousJSXLike) {
    v.invariant(allExtensions, "disallowAmbiguousJSXLike:true requires allExtensions:true");
  }

  const optimizeConstEnums = v.validateBooleanOption(TopLevelOptions.optimizeConstEnums, options.optimizeConstEnums, false);
  return {
    allExtensions,
    allowNamespaces,
    disallowAmbiguousJSXLike,
    isTSX,
    jsxPragma,
    jsxPragmaFrag,
    onlyRemoveTypeImports,
    optimizeConstEnums
  };
}

var index = helperPluginUtils.declarePreset((api, opts) => {
  api.assertVersion(7);
  const {
    allExtensions,
    allowNamespaces,
    disallowAmbiguousJSXLike,
    isTSX,
    jsxPragma,
    jsxPragmaFrag,
    onlyRemoveTypeImports,
    optimizeConstEnums
  } = normalizeOptions(opts);

  const pluginOptions = (isTSX, disallowAmbiguousJSXLike) => ({
    allowDeclareFields: opts.allowDeclareFields,
    allowNamespaces,
    disallowAmbiguousJSXLike,
    isTSX,
    jsxPragma,
    jsxPragmaFrag,
    onlyRemoveTypeImports,
    optimizeConstEnums
  });

  return {
    overrides: allExtensions ? [{
      plugins: [[transformTypeScript__default["default"], pluginOptions(isTSX, disallowAmbiguousJSXLike)]]
    }] : [{
      test: /\.ts$/,
      plugins: [[transformTypeScript__default["default"], pluginOptions(false, false)]]
    }, {
      test: /\.mts$/,
      sourceType: "module",
      plugins: [[transformTypeScript__default["default"], pluginOptions(false, true)]]
    }, {
      test: /\.cts$/,
      sourceType: "script",
      plugins: [[transformTypeScript__default["default"], pluginOptions(false, true)]]
    }, {
      test: /\.tsx$/,
      plugins: [[transformTypeScript__default["default"], pluginOptions(true, false)]]
    }]
  };
});

exports["default"] = index;
//# sourceMappingURL=index.js.map
