#!/usr/bin/env node
'use strict';

let hasBundled = true

try {
  require.resolve('./index.bundled.js');
} catch(err) {
  // We don't have/need this on legacy builds and dev builds
  // If an error happens here, throw it, that means no Node.js distribution exists at all.
  hasBundled = false;
}

const cli = !hasBundled ? require('../') : require('./index.bundled.js');

if (cli.autoRun) {
  return;
}

const run = cli.run || cli.cli || cli.default;
run(process.argv).catch(function (error) {
  console.error(`
${error.stack || error.message || error}
`);
  process.exit(1);
});
