"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.canSkipRegexpu = canSkipRegexpu;
exports.generateRegexpuOptions = generateRegexpuOptions;
exports.transformFlags = transformFlags;

var _features = require("./features");

function generateRegexpuOptions(toTransform) {
  const feat = (name, ok = "transform") => {
    return (0, _features.hasFeature)(toTransform, _features.FEATURES[name]) ? ok : false;
  };

  return {
    unicodeFlag: feat("unicodeFlag"),
    unicodeSetsFlag: feat("unicodeSetsFlag") || feat("unicodeSetsFlag_syntax", "parse"),
    dotAllFlag: feat("dotAllFlag"),
    unicodePropertyEscapes: feat("unicodePropertyEscape"),
    namedGroups: feat("namedCaptureGroups"),
    onNamedGroup: () => {}
  };
}

function canSkipRegexpu(node, options) {
  const {
    flags,
    pattern
  } = node;

  if (flags.includes("v")) {
    if (options.unicodeSetsFlag === "transform") return false;
  }

  if (flags.includes("u")) {
    if (options.unicodeFlag === "transform") return false;

    if (options.unicodePropertyEscapes === "transform" && /\\[pP]{/.test(pattern)) {
      return false;
    }
  }

  if (flags.includes("s")) {
    if (options.dotAllFlag === "transform") return false;
  }

  if (options.namedGroups === "transform" && /\(\?<(?![=!])/.test(pattern)) {
    return false;
  }

  return true;
}

function transformFlags(regexpuOptions, flags) {
  if (regexpuOptions.unicodeSetsFlag === "transform") {
    flags = flags.replace("v", "u");
  }

  if (regexpuOptions.unicodeFlag === "transform") {
    flags = flags.replace("u", "");
  }

  if (regexpuOptions.dotAllFlag === "transform") {
    flags = flags.replace("s", "");
  }

  return flags;
}