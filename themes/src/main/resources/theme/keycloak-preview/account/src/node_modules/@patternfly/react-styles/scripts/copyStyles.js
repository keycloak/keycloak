const { copySync } = require('fs-extra');
const { resolve, dirname, join } = require('path');

const toDir = resolve(__dirname, '../css');
const fromDir = dirname(require.resolve('@patternfly/patternfly/package.json'));

copySync(join(fromDir, 'assets/images'), join(toDir, 'assets/images'));
