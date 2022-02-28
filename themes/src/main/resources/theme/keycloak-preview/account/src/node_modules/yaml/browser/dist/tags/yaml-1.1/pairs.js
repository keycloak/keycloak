import { YAMLSemanticError } from '../../errors';
import YAMLMap from '../../schema/Map';
import Pair from '../../schema/Pair';
import parseSeq from '../../schema/parseSeq';
import YAMLSeq from '../../schema/Seq';
export function parsePairs(doc, cst) {
  var seq = parseSeq(doc, cst);

  for (var i = 0; i < seq.items.length; ++i) {
    var item = seq.items[i];
    if (item instanceof Pair) continue;else if (item instanceof YAMLMap) {
      if (item.items.length > 1) {
        var msg = 'Each pair must have its own sequence indicator';
        throw new YAMLSemanticError(cst, msg);
      }

      var pair = item.items[0] || new Pair();
      if (item.commentBefore) pair.commentBefore = pair.commentBefore ? "".concat(item.commentBefore, "\n").concat(pair.commentBefore) : item.commentBefore;
      if (item.comment) pair.comment = pair.comment ? "".concat(item.comment, "\n").concat(pair.comment) : item.comment;
      item = pair;
    }
    seq.items[i] = item instanceof Pair ? item : new Pair(item);
  }

  return seq;
}
export function createPairs(schema, iterable, ctx) {
  var pairs = new YAMLSeq(schema);
  pairs.tag = 'tag:yaml.org,2002:pairs';
  var _iteratorNormalCompletion = true;
  var _didIteratorError = false;
  var _iteratorError = undefined;

  try {
    for (var _iterator = iterable[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
      var it = _step.value;
      var key = void 0,
          value = void 0;

      if (Array.isArray(it)) {
        if (it.length === 2) {
          key = it[0];
          value = it[1];
        } else throw new TypeError("Expected [key, value] tuple: ".concat(it));
      } else if (it && it instanceof Object) {
        var keys = Object.keys(it);

        if (keys.length === 1) {
          key = keys[0];
          value = it[key];
        } else throw new TypeError("Expected { key: value } tuple: ".concat(it));
      } else {
        key = it;
      }

      var pair = schema.createPair(key, value, ctx);
      pairs.items.push(pair);
    }
  } catch (err) {
    _didIteratorError = true;
    _iteratorError = err;
  } finally {
    try {
      if (!_iteratorNormalCompletion && _iterator.return != null) {
        _iterator.return();
      }
    } finally {
      if (_didIteratorError) {
        throw _iteratorError;
      }
    }
  }

  return pairs;
}
export default {
  default: false,
  tag: 'tag:yaml.org,2002:pairs',
  resolve: parsePairs,
  createNode: createPairs
};