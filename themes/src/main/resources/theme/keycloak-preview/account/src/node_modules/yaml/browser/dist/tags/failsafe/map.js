import _typeof from "@babel/runtime/helpers/typeof";
import _slicedToArray from "@babel/runtime/helpers/slicedToArray";
import YAMLMap from '../../schema/Map';
import parseMap from '../../schema/parseMap';

function createMap(schema, obj, ctx) {
  var map = new YAMLMap(schema);

  if (obj instanceof Map) {
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
      for (var _iterator = obj[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
        var _step$value = _slicedToArray(_step.value, 2),
            key = _step$value[0],
            value = _step$value[1];

        map.items.push(schema.createPair(key, value, ctx));
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
  } else if (obj && _typeof(obj) === 'object') {
    for (var _i = 0, _Object$keys = Object.keys(obj); _i < _Object$keys.length; _i++) {
      var _key = _Object$keys[_i];
      map.items.push(schema.createPair(_key, obj[_key], ctx));
    }
  }

  if (typeof schema.sortMapEntries === 'function') {
    map.items.sort(schema.sortMapEntries);
  }

  return map;
}

export default {
  createNode: createMap,
  default: true,
  nodeClass: YAMLMap,
  tag: 'tag:yaml.org,2002:map',
  resolve: parseMap
};