import _slicedToArray from "@babel/runtime/helpers/slicedToArray";
import parseCST from './cst/parse';
import Document from './Document'; // test harness for yaml-test-suite event tests

export default function testEvents(src, options) {
  var opt = Object.assign({
    keepCstNodes: true,
    keepNodeTypes: true,
    version: '1.2'
  }, options);
  var docs = parseCST(src).map(function (cstDoc) {
    return new Document(opt).parse(cstDoc);
  });
  var errDoc = docs.find(function (doc) {
    return doc.errors.length > 0;
  });
  var error = errDoc ? errDoc.errors[0].message : null;
  var events = ['+STR'];

  try {
    for (var i = 0; i < docs.length; ++i) {
      var doc = docs[i];
      var root = doc.contents;
      if (Array.isArray(root)) root = root[0];

      var _ref = doc.range || [0, 0],
          _ref2 = _slicedToArray(_ref, 2),
          rootStart = _ref2[0],
          rootEnd = _ref2[1];

      var e = doc.errors[0] && doc.errors[0].source;
      if (e && e.type === 'SEQ_ITEM') e = e.node;
      if (e && (e.type === 'DOCUMENT' || e.range.start < rootStart)) throw new Error();
      var docStart = '+DOC';
      var pre = src.slice(0, rootStart);
      var explicitDoc = /---\s*$/.test(pre);
      if (explicitDoc) docStart += ' ---';else if (!doc.contents) continue;
      events.push(docStart);
      addEvents(events, doc, e, root);
      if (doc.contents && doc.contents.length > 1) throw new Error();
      var docEnd = '-DOC';

      if (rootEnd) {
        var post = src.slice(rootEnd);
        if (/^\.\.\./.test(post)) docEnd += ' ...';
      }

      events.push(docEnd);
    }
  } catch (e) {
    return {
      events: events,
      error: error || e
    };
  }

  events.push('-STR');
  return {
    events: events,
    error: error
  };
}

function addEvents(events, doc, e, node) {
  if (!node) {
    events.push('=VAL :');
    return;
  }

  if (e && node.cstNode === e) throw new Error();
  var props = '';
  var anchor = doc.anchors.getName(node);

  if (anchor) {
    if (/\d$/.test(anchor)) {
      var alt = anchor.replace(/\d$/, '');
      if (doc.anchors.getNode(alt)) anchor = alt;
    }

    props = " &".concat(anchor);
  }

  if (node.cstNode && node.cstNode.tag) {
    var _node$cstNode$tag = node.cstNode.tag,
        handle = _node$cstNode$tag.handle,
        suffix = _node$cstNode$tag.suffix;
    props += handle === '!' && !suffix ? ' <!>' : " <".concat(node.tag, ">");
  }

  var scalar = null;

  switch (node.type) {
    case 'ALIAS':
      {
        var alias = doc.anchors.getName(node.source);

        if (/\d$/.test(alias)) {
          var _alt = alias.replace(/\d$/, '');

          if (doc.anchors.getNode(_alt)) alias = _alt;
        }

        events.push("=ALI".concat(props, " *").concat(alias));
      }
      break;

    case 'BLOCK_FOLDED':
      scalar = '>';
      break;

    case 'BLOCK_LITERAL':
      scalar = '|';
      break;

    case 'PLAIN':
      scalar = ':';
      break;

    case 'QUOTE_DOUBLE':
      scalar = '"';
      break;

    case 'QUOTE_SINGLE':
      scalar = "'";
      break;

    case 'PAIR':
      events.push("+MAP".concat(props));
      addEvents(events, doc, e, node.key);
      addEvents(events, doc, e, node.value);
      events.push('-MAP');
      break;

    case 'FLOW_SEQ':
    case 'SEQ':
      events.push("+SEQ".concat(props));
      node.items.forEach(function (item) {
        addEvents(events, doc, e, item);
      });
      events.push('-SEQ');
      break;

    case 'FLOW_MAP':
    case 'MAP':
      events.push("+MAP".concat(props));
      node.items.forEach(function (_ref3) {
        var key = _ref3.key,
            value = _ref3.value;
        addEvents(events, doc, e, key);
        addEvents(events, doc, e, value);
      });
      events.push('-MAP');
      break;

    default:
      throw new Error("Unexpected node type ".concat(node.type));
  }

  if (scalar) {
    var value = node.cstNode.strValue.replace(/\\/g, '\\\\').replace(/\0/g, '\\0').replace(/\x07/g, '\\a').replace(/\x08/g, '\\b').replace(/\t/g, '\\t').replace(/\n/g, '\\n').replace(/\v/g, '\\v').replace(/\f/g, '\\f').replace(/\r/g, '\\r').replace(/\x1b/g, '\\e');
    events.push("=VAL".concat(props, " ").concat(scalar).concat(value));
  }
}