(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(['glob', 'fs'], factory);
  } else if (typeof exports !== "undefined") {
    factory(require('glob'), require('fs'));
  } else {
    var mod = {
      exports: {}
    };
    factory(global.glob, global.fs);
    global.undefined = mod.exports;
  }
})(this, function (glob, fs) {
  "use strict";

  glob.sync('css/**/*.ts', {
    ignore: ['**/*.d.ts']
  }).forEach(file => fs.unlinkSync(file));
});
//# sourceMappingURL=removeTS.js.map