'use strict';
const cmp = require('semver/functions/cmp');
const semver = require('semver/functions/coerce');

const has = Function.call.bind({}.hasOwnProperty);

function compare(a, operator, b) {
  return cmp(semver(a), operator, semver(b));
}

function filterOutStabilizedProposals(modules) {
  const modulesSet = new Set(modules);

  for (const $module of modulesSet) {
    if ($module.startsWith('esnext.') && modulesSet.has($module.replace(/^esnext\./, 'es.'))) {
      modulesSet.delete($module);
    }
  }

  return [...modulesSet];
}

function intersection(list, order) {
  const set = list instanceof Set ? list : new Set(list);
  return order.filter(name => set.has(name));
}

function sortObjectByKey(object, fn) {
  return Object.keys(object).sort(fn).reduce((memo, key) => {
    memo[key] = object[key];
    return memo;
  }, {});
}

module.exports = {
  compare,
  filterOutStabilizedProposals,
  has,
  intersection,
  semver,
  sortObjectByKey,
};
