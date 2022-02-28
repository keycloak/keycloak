import _typeof from "@babel/runtime/helpers/typeof";
import Collection from './schema/Collection';
import Pair from './schema/Pair';
import Scalar from './schema/Scalar';

var visit = function visit(node, tags) {
  if (node && _typeof(node) === 'object') {
    var tag = node.tag;

    if (node instanceof Collection) {
      if (tag) tags[tag] = true;
      node.items.forEach(function (n) {
        return visit(n, tags);
      });
    } else if (node instanceof Pair) {
      visit(node.key, tags);
      visit(node.value, tags);
    } else if (node instanceof Scalar) {
      if (tag) tags[tag] = true;
    }
  }

  return tags;
};

export default (function (node) {
  return Object.keys(visit(node, {}));
});