import { Type } from '../constants';
import { YAMLSemanticError, YAMLSyntaxError, YAMLWarning } from '../errors';
import Pair from './Pair';
import { checkFlowCollectionEnd, checkKeyLength, resolveComments } from './parseUtils';
import Seq from './Seq';
import Collection from './Collection';
export default function parseSeq(doc, cst) {
  if (cst.type !== Type.SEQ && cst.type !== Type.FLOW_SEQ) {
    var msg = "A ".concat(cst.type, " node cannot be resolved as a sequence");
    doc.errors.push(new YAMLSyntaxError(cst, msg));
    return null;
  }

  var _ref = cst.type === Type.FLOW_SEQ ? resolveFlowSeqItems(doc, cst) : resolveBlockSeqItems(doc, cst),
      comments = _ref.comments,
      items = _ref.items;

  var seq = new Seq();
  seq.items = items;
  resolveComments(seq, comments);

  if (!doc.options.mapAsMap && items.some(function (it) {
    return it instanceof Pair && it.key instanceof Collection;
  })) {
    var warn = 'Keys with collection values will be stringified as YAML due to JS Object restrictions. Use mapAsMap: true to avoid this.';
    doc.warnings.push(new YAMLWarning(cst, warn));
  }

  cst.resolved = seq;
  return seq;
}

function resolveBlockSeqItems(doc, cst) {
  var comments = [];
  var items = [];

  for (var i = 0; i < cst.items.length; ++i) {
    var item = cst.items[i];

    switch (item.type) {
      case Type.BLANK_LINE:
        comments.push({
          before: items.length
        });
        break;

      case Type.COMMENT:
        comments.push({
          comment: item.comment,
          before: items.length
        });
        break;

      case Type.SEQ_ITEM:
        if (item.error) doc.errors.push(item.error);
        items.push(doc.resolveNode(item.node));

        if (item.hasProps) {
          var msg = 'Sequence items cannot have tags or anchors before the - indicator';
          doc.errors.push(new YAMLSemanticError(item, msg));
        }

        break;

      default:
        if (item.error) doc.errors.push(item.error);
        doc.errors.push(new YAMLSyntaxError(item, "Unexpected ".concat(item.type, " node in sequence")));
    }
  }

  return {
    comments: comments,
    items: items
  };
}

function resolveFlowSeqItems(doc, cst) {
  var comments = [];
  var items = [];
  var explicitKey = false;
  var key = undefined;
  var keyStart = null;
  var next = '[';

  for (var i = 0; i < cst.items.length; ++i) {
    var item = cst.items[i];

    if (typeof item.char === 'string') {
      var char = item.char,
          offset = item.offset;

      if (char !== ':' && (explicitKey || key !== undefined)) {
        if (explicitKey && key === undefined) key = next ? items.pop() : null;
        items.push(new Pair(key));
        explicitKey = false;
        key = undefined;
        keyStart = null;
      }

      if (char === next) {
        next = null;
      } else if (!next && char === '?') {
        explicitKey = true;
      } else if (next !== '[' && char === ':' && key === undefined) {
        if (next === ',') {
          key = items.pop();

          if (key instanceof Pair) {
            var msg = 'Chaining flow sequence pairs is invalid';
            var err = new YAMLSemanticError(cst, msg);
            err.offset = offset;
            doc.errors.push(err);
          }

          if (!explicitKey) checkKeyLength(doc.errors, cst, i, key, keyStart);
        } else {
          key = null;
        }

        keyStart = null;
        explicitKey = false; // TODO: add error for non-explicit multiline plain key

        next = null;
      } else if (next === '[' || char !== ']' || i < cst.items.length - 1) {
        var _msg = "Flow sequence contains an unexpected ".concat(char);

        var _err = new YAMLSyntaxError(cst, _msg);

        _err.offset = offset;
        doc.errors.push(_err);
      }
    } else if (item.type === Type.BLANK_LINE) {
      comments.push({
        before: items.length
      });
    } else if (item.type === Type.COMMENT) {
      comments.push({
        comment: item.comment,
        before: items.length
      });
    } else {
      if (next) {
        var _msg2 = "Expected a ".concat(next, " in flow sequence");

        doc.errors.push(new YAMLSemanticError(item, _msg2));
      }

      var value = doc.resolveNode(item);

      if (key === undefined) {
        items.push(value);
      } else {
        items.push(new Pair(key, value));
        key = undefined;
      }

      keyStart = item.range.start;
      next = ',';
    }
  }

  checkFlowCollectionEnd(doc.errors, cst);
  if (key !== undefined) items.push(new Pair(key));
  return {
    comments: comments,
    items: items
  };
}