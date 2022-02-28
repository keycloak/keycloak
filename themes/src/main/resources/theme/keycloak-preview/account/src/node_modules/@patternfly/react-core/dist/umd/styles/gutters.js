(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.GutterSize = undefined;
  exports.getGutterModifier = getGutterModifier;
  const GutterSize = exports.GutterSize = {
    sm: 'sm',
    md: 'md',
    lg: 'lg'
  };
  /**
   * @param {any} styleObj - Style object
   * @param {'sm' | 'md' | 'lg'} size - Size string 'sm', 'md', or 'lg'
   * @param {any} defaultValue - Default value
   */

  function getGutterModifier(styleObj, size, defaultValue) {
    return (0, _reactStyles.getModifier)(styleObj, `gutter-${size}`, defaultValue);
  }
});
//# sourceMappingURL=gutters.js.map