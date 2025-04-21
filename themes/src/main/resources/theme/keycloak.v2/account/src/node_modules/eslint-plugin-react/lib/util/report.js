'use strict';

const semver = require('semver');
const eslintPkg = require('eslint/package.json');

module.exports = function report(context, message, messageId, data) {
  context.report(
    Object.assign(
      messageId && semver.satisfies(eslintPkg.version, '>= 4.15') ? { messageId } : { message },
      data
    )
  );
};
