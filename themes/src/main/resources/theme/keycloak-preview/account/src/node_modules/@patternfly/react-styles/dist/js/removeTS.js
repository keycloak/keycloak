"use strict";

var glob = require('glob');

var fs = require('fs');

glob.sync('css/**/*.ts', {
  ignore: ['**/*.d.ts']
}).forEach(function (file) {
  return fs.unlinkSync(file);
});
//# sourceMappingURL=removeTS.js.map