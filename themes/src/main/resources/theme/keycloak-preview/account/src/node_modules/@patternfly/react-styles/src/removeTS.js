const glob = require('glob');
const fs = require('fs');

glob.sync('css/**/*.ts', { ignore: ['**/*.d.ts'] }).forEach(file => fs.unlinkSync(file));
