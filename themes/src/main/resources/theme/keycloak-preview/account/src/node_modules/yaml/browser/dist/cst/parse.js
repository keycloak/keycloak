// Published as 'yaml/parse-cst'
import Document from './Document';
import ParseContext from './ParseContext';
export default function parse(src) {
  var cr = [];

  if (src.indexOf('\r') !== -1) {
    src = src.replace(/\r\n?/g, function (match, offset) {
      if (match.length > 1) cr.push(offset);
      return '\n';
    });
  }

  var documents = [];
  var offset = 0;

  do {
    var doc = new Document();
    var context = new ParseContext({
      src: src
    });
    offset = doc.parse(context, offset);
    documents.push(doc);
  } while (offset < src.length);

  documents.setOrigRanges = function () {
    if (cr.length === 0) return false;

    for (var i = 1; i < cr.length; ++i) {
      cr[i] -= i;
    }

    var crOffset = 0;

    for (var _i = 0; _i < documents.length; ++_i) {
      crOffset = documents[_i].setOrigRanges(cr, crOffset);
    }

    cr.splice(0, cr.length);
    return true;
  };

  documents.toString = function () {
    return documents.join('...\n');
  };

  return documents;
}