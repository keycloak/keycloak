/**
 * @fileoverview Utility functions for React and Flow version configuration
 * @author Yannick Croissant
 */
'use strict';

const resolve = require('resolve');
const error = require('./error');

let warnedForMissingVersion = false;

function detectReactVersion() {
  try {
    const reactPath = resolve.sync('react', {basedir: process.cwd()});
    const react = require(reactPath);
    return react.version;
  } catch (e) {
    if (e.code === 'MODULE_NOT_FOUND') {
      error('Warning: React version was set to "detect" in eslint-plugin-react settings, ' +
        'but the "react" package is not installed. Assuming latest React version for linting.');
      return '999.999.999';
    }
    throw e;
  }
}

function getReactVersionFromContext(context) {
  let confVer = '999.999.999';
  // .eslintrc shared settings (http://eslint.org/docs/user-guide/configuring#adding-shared-settings)
  if (context.settings.react && context.settings.react.version) {
    let settingsVersion = context.settings.react.version;
    if (settingsVersion === 'detect') {
      settingsVersion = detectReactVersion();
    }
    if (typeof settingsVersion !== 'string') {
      error('Warning: React version specified in eslint-plugin-react-settings must be a string; ' +
        `got “${typeof settingsVersion}”`);
    }
    confVer = String(settingsVersion);
  } else if (!warnedForMissingVersion) {
    error('Warning: React version not specified in eslint-plugin-react settings. ' +
      'See https://github.com/yannickcr/eslint-plugin-react#configuration .');
    warnedForMissingVersion = true;
  }
  confVer = /^[0-9]+\.[0-9]+$/.test(confVer) ? `${confVer}.0` : confVer;
  return confVer.split('.').map(part => Number(part));
}

function getFlowVersionFromContext(context) {
  let confVer = '999.999.999';
  // .eslintrc shared settings (http://eslint.org/docs/user-guide/configuring#adding-shared-settings)
  if (context.settings.react && context.settings.react.flowVersion) {
    const flowVersion = context.settings.react.flowVersion;
    if (typeof flowVersion !== 'string') {
      error('Warning: Flow version specified in eslint-plugin-react-settings must be a string; ' +
        `got “${typeof flowVersion}”`);
    }
    confVer = String(flowVersion);
  } else {
    throw 'Could not retrieve flowVersion from settings';
  }
  confVer = /^[0-9]+\.[0-9]+$/.test(confVer) ? `${confVer}.0` : confVer;
  return confVer.split('.').map(part => Number(part));
}

function normalizeParts(parts) {
  return Array.from({length: 3}, (_, i) => (parts[i] || 0));
}

function test(context, methodVer, confVer) {
  const methodVers = normalizeParts(String(methodVer || '').split('.').map(part => Number(part)));
  const confVers = normalizeParts(confVer);
  const higherMajor = methodVers[0] < confVers[0];
  const higherMinor = methodVers[0] === confVers[0] && methodVers[1] < confVers[1];
  const higherOrEqualPatch = methodVers[0] === confVers[0]
    && methodVers[1] === confVers[1]
    && methodVers[2] <= confVers[2];

  return higherMajor || higherMinor || higherOrEqualPatch;
}

function testReactVersion(context, methodVer) {
  return test(context, methodVer, getReactVersionFromContext(context));
}

function testFlowVersion(context, methodVer) {
  return test(context, methodVer, getFlowVersionFromContext(context));
}

module.exports = {
  testReactVersion,
  testFlowVersion
};
