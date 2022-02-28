"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _Collection = _interopRequireDefault(require("./schema/Collection"));

var _Pair = _interopRequireDefault(require("./schema/Pair"));

var _Scalar = _interopRequireDefault(require("./schema/Scalar"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const visit = (node, tags) => {
  if (node && typeof node === 'object') {
    const {
      tag
    } = node;

    if (node instanceof _Collection.default) {
      if (tag) tags[tag] = true;
      node.items.forEach(n => visit(n, tags));
    } else if (node instanceof _Pair.default) {
      visit(node.key, tags);
      visit(node.value, tags);
    } else if (node instanceof _Scalar.default) {
      if (tag) tags[tag] = true;
    }
  }

  return tags;
};

var _default = node => Object.keys(visit(node, {}));

exports.default = _default;