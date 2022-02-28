import parseSeq from '../../schema/parseSeq';
import YAMLSeq from '../../schema/Seq';

function createSeq(schema, obj, ctx) {
  var seq = new YAMLSeq(schema);

  if (obj && obj[Symbol.iterator]) {
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
      for (var _iterator = obj[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
        var it = _step.value;
        var v = schema.createNode(it, ctx.wrapScalars, null, ctx);
        seq.items.push(v);
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
  }

  return seq;
}

export default {
  createNode: createSeq,
  default: true,
  nodeClass: YAMLSeq,
  tag: 'tag:yaml.org,2002:seq',
  resolve: parseSeq
};