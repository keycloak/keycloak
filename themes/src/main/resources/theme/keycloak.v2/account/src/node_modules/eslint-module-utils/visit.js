'use strict';
exports.__esModule = true;

exports.default = function visit(node, keys, visitorSpec) {
  if (!node || !keys) {
    return;
  }
  const type = node.type;
  if (typeof visitorSpec[type] === 'function') {
    visitorSpec[type](node);
  }
  const childFields = keys[type];
  if (!childFields) {
    return;
  }
  childFields.forEach((fieldName) => {
    [].concat(node[fieldName]).forEach((item) => {
      visit(item, keys, visitorSpec);
    });
  });
  if (typeof visitorSpec[`${type}:Exit`] === 'function') {
    visitorSpec[`${type}:Exit`](node);
  }
};
