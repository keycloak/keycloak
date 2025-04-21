'use strict';
const browserslist = require('browserslist');
const { compare, has } = require('./helpers');
const external = require('./external');

const aliases = new Map([
  ['and_chr', 'chrome'],
  ['and_ff', 'firefox'],
  ['ie_mob', 'ie'],
  ['ios_saf', 'ios'],
  ['op_mob', 'opera_mobile'],
]);

const validTargets = new Set([
  'android',
  'chrome',
  'deno',
  'edge',
  'electron',
  'firefox',
  'ie',
  'ios',
  'node',
  'opera',
  'opera_mobile',
  'phantom',
  'rhino',
  'safari',
  'samsung',
]);

const toLowerKeys = function (object) {
  return Object.entries(object).reduce((accumulator, [key, value]) => {
    accumulator[key.toLowerCase()] = value;
    return accumulator;
  }, {});
};

module.exports = function (targets) {
  const { browsers, esmodules, node, ...rest } = (typeof targets != 'object' || Array.isArray(targets))
    ? { browsers: targets } : toLowerKeys(targets);

  const list = Object.entries(rest);

  if (browsers) {
    if (typeof browsers == 'string' || Array.isArray(browsers)) {
      list.push(...browserslist(browsers).map(it => it.split(' ')));
    } else {
      list.push(...Object.entries(browsers));
    }
  }
  if (esmodules) {
    list.push(...Object.entries(external.modules));
  }
  if (node) {
    list.push(['node', node === 'current' ? process.versions.node : node]);
  }

  const normalized = list.map(([engine, version]) => {
    if (has(browserslist.aliases, engine)) {
      engine = browserslist.aliases[engine];
    }
    if (aliases.has(engine)) {
      engine = aliases.get(engine);
    }
    return [engine, String(version)];
  }).filter(([engine]) => {
    return validTargets.has(engine);
  }).sort(([a], [b]) => {
    return a < b ? -1 : a > b ? 1 : 0;
  });

  const reducedByMinVersion = new Map();
  for (const [engine, version] of normalized) {
    if (!reducedByMinVersion.has(engine) || compare(version, '<=', reducedByMinVersion.get(engine))) {
      reducedByMinVersion.set(engine, version);
    }
  }

  return reducedByMinVersion;
};
