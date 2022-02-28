import _classCallCheck from "@babel/runtime/helpers/classCallCheck";
import _possibleConstructorReturn from "@babel/runtime/helpers/possibleConstructorReturn";
import _getPrototypeOf from "@babel/runtime/helpers/getPrototypeOf";
import _inherits from "@babel/runtime/helpers/inherits";
import parseCST from './cst/parse';
import YAMLDocument from './Document';
import { YAMLSemanticError } from './errors';
import Schema from './schema';
import { warn } from './warnings';
var defaultOptions = {
  anchorPrefix: 'a',
  customTags: null,
  keepCstNodes: false,
  keepNodeTypes: true,
  keepBlobsInJSON: true,
  mapAsMap: false,
  maxAliasCount: 100,
  prettyErrors: false,
  // TODO Set true in v2
  simpleKeys: false,
  version: '1.2'
};

function createNode(value) {
  var wrapScalars = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true;
  var tag = arguments.length > 2 ? arguments[2] : undefined;

  if (tag === undefined && typeof wrapScalars === 'string') {
    tag = wrapScalars;
    wrapScalars = true;
  }

  var options = Object.assign({}, YAMLDocument.defaults[defaultOptions.version], defaultOptions);
  var schema = new Schema(options);
  return schema.createNode(value, wrapScalars, tag);
}

var Document = /*#__PURE__*/function (_YAMLDocument) {
  _inherits(Document, _YAMLDocument);

  function Document(options) {
    _classCallCheck(this, Document);

    return _possibleConstructorReturn(this, _getPrototypeOf(Document).call(this, Object.assign({}, defaultOptions, options)));
  }

  return Document;
}(YAMLDocument);

function parseAllDocuments(src, options) {
  var stream = [];
  var prev;
  var _iteratorNormalCompletion = true;
  var _didIteratorError = false;
  var _iteratorError = undefined;

  try {
    for (var _iterator = parseCST(src)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
      var cstDoc = _step.value;
      var doc = new Document(options);
      doc.parse(cstDoc, prev);
      stream.push(doc);
      prev = doc;
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

  return stream;
}

function parseDocument(src, options) {
  var cst = parseCST(src);
  var doc = new Document(options).parse(cst[0]);

  if (cst.length > 1) {
    var errMsg = 'Source contains multiple documents; please use YAML.parseAllDocuments()';
    doc.errors.unshift(new YAMLSemanticError(cst[1], errMsg));
  }

  return doc;
}

function parse(src, options) {
  var doc = parseDocument(src, options);
  doc.warnings.forEach(function (warning) {
    return warn(warning);
  });
  if (doc.errors.length > 0) throw doc.errors[0];
  return doc.toJSON();
}

function stringify(value, options) {
  var doc = new Document(options);
  doc.contents = value;
  return String(doc);
}

export default {
  createNode: createNode,
  defaultOptions: defaultOptions,
  Document: Document,
  parse: parse,
  parseAllDocuments: parseAllDocuments,
  parseCST: parseCST,
  parseDocument: parseDocument,
  stringify: stringify
};