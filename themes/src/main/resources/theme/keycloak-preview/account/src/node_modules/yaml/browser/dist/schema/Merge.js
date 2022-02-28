import _slicedToArray from "@babel/runtime/helpers/slicedToArray";
import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _createClass from "@babel/runtime/helpers/createClass";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _get from "@babel/runtime/helpers/get";
import _inherits from "@babel/runtime/helpers/inherits";
import YAMLMap from './Map';
import Pair from './Pair';
import Scalar from './Scalar';
import Seq from './Seq';
export var MERGE_KEY = '<<';

var Merge = /*#__PURE__*/function (_Pair) {
  _inherits(Merge, _Pair);

  function Merge(pair) {
    var _this;

    _classCallCheck(this, Merge);

    if (pair instanceof Pair) {
      var seq = pair.value;

      if (!(seq instanceof Seq)) {
        seq = new Seq();
        seq.items.push(pair.value);
        seq.range = pair.value.range;
      }

      _this = _possibleConstructorReturn(this, _getPrototypeOf(Merge).call(this, pair.key, seq));
      _this.range = pair.range;
    } else {
      _this = _possibleConstructorReturn(this, _getPrototypeOf(Merge).call(this, new Scalar(MERGE_KEY), new Seq()));
    }

    _this.type = 'MERGE_PAIR';
    return _possibleConstructorReturn(_this);
  } // If the value associated with a merge key is a single mapping node, each of
  // its key/value pairs is inserted into the current mapping, unless the key
  // already exists in it. If the value associated with the merge key is a
  // sequence, then this sequence is expected to contain mapping nodes and each
  // of these nodes is merged in turn according to its order in the sequence.
  // Keys in mapping nodes earlier in the sequence override keys specified in
  // later mapping nodes. -- http://yaml.org/type/merge.html


  _createClass(Merge, [{
    key: "addToJSMap",
    value: function addToJSMap(ctx, map) {
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = this.value.items[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var source = _step.value.source;
          if (!(source instanceof YAMLMap)) throw new Error('Merge sources must be maps');
          var srcMap = source.toJSON(null, ctx, Map);
          var _iteratorNormalCompletion2 = true;
          var _didIteratorError2 = false;
          var _iteratorError2 = undefined;

          try {
            for (var _iterator2 = srcMap[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
              var _step2$value = _slicedToArray(_step2.value, 2),
                  key = _step2$value[0],
                  value = _step2$value[1];

              if (map instanceof Map) {
                if (!map.has(key)) map.set(key, value);
              } else if (map instanceof Set) {
                map.add(key);
              } else {
                if (!Object.prototype.hasOwnProperty.call(map, key)) map[key] = value;
              }
            }
          } catch (err) {
            _didIteratorError2 = true;
            _iteratorError2 = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion2 && _iterator2.return != null) {
                _iterator2.return();
              }
            } finally {
              if (_didIteratorError2) {
                throw _iteratorError2;
              }
            }
          }
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

      return map;
    }
  }, {
    key: "toString",
    value: function toString(ctx, onComment) {
      var seq = this.value;
      if (seq.items.length > 1) return _get(_getPrototypeOf(Merge.prototype), "toString", this).call(this, ctx, onComment);
      this.value = seq.items[0];

      var str = _get(_getPrototypeOf(Merge.prototype), "toString", this).call(this, ctx, onComment);

      this.value = seq;
      return str;
    }
  }]);

  return Merge;
}(Pair);

export { Merge as default };