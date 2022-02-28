import { SourceMapGenerator } from 'source-map';
import convert from 'convert-source-map';
import findRoot from 'find-root';
import memoize from '@emotion/memoize';
import nodePath from 'path';
import hashString from '@emotion/hash';
import { serializeStyles } from '@emotion/serialize';

// babel-plugin-styled-components
// https://github.com/styled-components/babel-plugin-styled-components/blob/8d44acc36f067d60d4e09f9c22ff89695bc332d2/src/minify/index.js
var multilineCommentRegex = /\/\*[^!](.|[\r\n])*?\*\//g;
var lineCommentStart = /\/\//g;
var symbolRegex = /(\s*[;:{},]\s*)/g; // Counts occurences of substr inside str

var countOccurences = function countOccurences(str, substr) {
  return str.split(substr).length - 1;
}; // Joins substrings until predicate returns true


var reduceSubstr = function reduceSubstr(substrs, join, predicate) {
  var length = substrs.length;
  var res = substrs[0];

  if (length === 1) {
    return res;
  }

  for (var i = 1; i < length; i++) {
    if (predicate(res)) {
      break;
    }

    res += join + substrs[i];
  }

  return res;
}; // Joins at comment starts when it's inside a string or parantheses
// effectively removing line comments


var stripLineComment = function stripLineComment(line) {
  return reduceSubstr(line.split(lineCommentStart), '//', function (str) {
    return !str.endsWith(':') && // NOTE: This is another guard against urls, if they're not inside strings or parantheses.
    countOccurences(str, "'") % 2 === 0 && countOccurences(str, '"') % 2 === 0 && countOccurences(str, '(') === countOccurences(str, ')');
  });
};
var compressSymbols = function compressSymbols(code) {
  return code.split(symbolRegex).reduce(function (str, fragment, index) {
    // Even-indices are non-symbol fragments
    if (index % 2 === 0) {
      return str + fragment;
    } // Only manipulate symbols outside of strings


    if (countOccurences(str, "'") % 2 === 0 && countOccurences(str, '"') % 2 === 0) {
      return str + fragment.trim();
    }

    return str + fragment;
  }, '');
}; // Detects lines that are exclusively line comments

var isLineComment = function isLineComment(line) {
  return line.trim().startsWith('//');
};

var linebreakRegex = /[\r\n]\s*/g;
var minify = function minify(code) {
  var newCode = code.replace(multilineCommentRegex, '\n') // Remove multiline comments
  .split(linebreakRegex) // Split at newlines
  .filter(function (line) {
    return line.length > 0 && !isLineComment(line);
  }) // Removes lines containing only line comments
  .map(stripLineComment) // Remove line comments inside text
  .join(' '); // Rejoin all lines

  return compressSymbols(newCode);
};

function getExpressionsFromTemplateLiteral(node, t) {
  var raw = createRawStringFromTemplateLiteral(node);
  var minified = minify(raw);
  return replacePlaceholdersWithExpressions(minified, node.expressions || [], t);
}

var interleave = function interleave(strings, interpolations) {
  return interpolations.reduce(function (array, interp, i) {
    return array.concat([interp], strings[i + 1]);
  }, [strings[0]]);
};

function getDynamicMatches(str) {
  var re = /xxx(\d+)xxx/gm;
  var match;
  var matches = [];

  while ((match = re.exec(str)) !== null) {
    // so that flow doesn't complain
    if (match !== null) {
      matches.push({
        value: match[0],
        p1: parseInt(match[1], 10),
        index: match.index
      });
    }
  }

  return matches;
}

function replacePlaceholdersWithExpressions(str, expressions, t) {
  var matches = getDynamicMatches(str);

  if (matches.length === 0) {
    if (str === '') {
      return [];
    }

    return [t.stringLiteral(str)];
  }

  var strings = [];
  var finalExpressions = [];
  var cursor = 0;
  matches.forEach(function (_ref, i) {
    var value = _ref.value,
        p1 = _ref.p1,
        index = _ref.index;
    var preMatch = str.substring(cursor, index);
    cursor = cursor + preMatch.length + value.length;

    if (preMatch) {
      strings.push(t.stringLiteral(preMatch));
    } else if (i === 0) {
      strings.push(t.stringLiteral(''));
    }

    finalExpressions.push(expressions[p1]);

    if (i === matches.length - 1) {
      strings.push(t.stringLiteral(str.substring(index + value.length)));
    }
  });
  return interleave(strings, finalExpressions).filter(function (node) {
    return node.value !== '';
  });
}

function createRawStringFromTemplateLiteral(quasi) {
  var strs = quasi.quasis.map(function (x) {
    return x.value.cooked;
  });
  var src = strs.reduce(function (arr, str, i) {
    arr.push(str);

    if (i !== strs.length - 1) {
      arr.push("xxx" + i + "xxx");
    }

    return arr;
  }, []).join('').trim();
  return src;
}

function getLabelFromPath(path, t) {
  return getIdentifierName(path, t);
}

function getDeclaratorName(path, t) {
  // $FlowFixMe
  var parent = path.findParent(function (p) {
    return p.isVariableDeclarator();
  });
  return parent && t.isIdentifier(parent.node.id) ? parent.node.id.name : '';
}

function getIdentifierName(path, t) {
  var classOrClassPropertyParent;

  if (t.isObjectProperty(path.parentPath) && path.parentPath.node.computed === false && (t.isIdentifier(path.parentPath.node.key) || t.isStringLiteral(path.parentPath.node.key))) {
    return path.parentPath.node.key.name || path.parentPath.node.key.value;
  }

  if (path) {
    // $FlowFixMe
    classOrClassPropertyParent = path.findParent(function (p) {
      return t.isClassProperty(p) || t.isClass(p);
    });
  }

  if (classOrClassPropertyParent) {
    if (t.isClassProperty(classOrClassPropertyParent) && classOrClassPropertyParent.node.computed === false && t.isIdentifier(classOrClassPropertyParent.node.key)) {
      return classOrClassPropertyParent.node.key.name;
    }

    if (t.isClass(classOrClassPropertyParent) && classOrClassPropertyParent.node.id) {
      return t.isIdentifier(classOrClassPropertyParent.node.id) ? classOrClassPropertyParent.node.id.name : '';
    }
  }

  var declaratorName = getDeclaratorName(path, t); // if the name starts with _ it was probably generated by babel so we should ignore it

  if (declaratorName.charAt(0) === '_') {
    return '';
  }

  return declaratorName;
}

function getGeneratorOpts(file) {
  return file.opts.generatorOpts ? file.opts.generatorOpts : file.opts;
}

function makeSourceMapGenerator(file) {
  var generatorOpts = getGeneratorOpts(file);
  var filename = generatorOpts.sourceFileName;
  var generator = new SourceMapGenerator({
    file: filename,
    sourceRoot: generatorOpts.sourceRoot
  });
  generator.setSourceContent(filename, file.code);
  return generator;
}
function getSourceMap(offset, state) {
  var generator = makeSourceMapGenerator(state.file);
  var generatorOpts = getGeneratorOpts(state.file);
  generator.addMapping({
    generated: {
      line: 1,
      column: 0
    },
    source: generatorOpts.sourceFileName,
    original: offset
  });
  return convert.fromObject(generator).toComment({
    multiline: true
  });
}

var hashArray = function hashArray(arr) {
  return hashString(arr.join(''));
};

var unsafeRequire = require;
var getPackageRootPath = memoize(function (filename) {
  return findRoot(filename);
});
function getTargetClassName(state, t) {
  if (state.emotionTargetClassNameCount === undefined) {
    state.emotionTargetClassNameCount = 0;
  }

  var filename = state.file.opts.filename; // normalize the file path to ignore folder structure
  // outside the current node project and arch-specific delimiters

  var moduleName = '';
  var rootPath = filename;

  try {
    rootPath = getPackageRootPath(filename);
    moduleName = unsafeRequire(rootPath + '/package.json').name;
  } catch (err) {}

  var finalPath = filename === rootPath ? nodePath.basename(filename) : filename.slice(rootPath.length);
  var positionInFile = state.emotionTargetClassNameCount++;
  var stuffToHash = [moduleName];

  if (finalPath) {
    stuffToHash.push(nodePath.normalize(finalPath));
  } else {
    stuffToHash.push(state.file.code);
  }

  var stableClassName = "e" + hashArray(stuffToHash) + positionInFile;
  return stableClassName;
}

// it's meant to simplify the most common cases so i don't want to make it especially complex
// also, this will be unnecessary when prepack is ready

function simplifyObject(node, t) {
  var bailout = false;
  var finalString = '';
  node.properties.forEach(function (property) {
    var _ref;

    if (bailout) {
      return;
    }

    if (property.computed || !t.isIdentifier(property.key) && !t.isStringLiteral(property.key) || !t.isStringLiteral(property.value) && !t.isNumericLiteral(property.value) && !t.isObjectExpression(property.value)) {
      bailout = true;
    }

    var key = property.key.name || property.key.value;

    if (key === 'styles') {
      bailout = true;
      return;
    }

    if (t.isObjectExpression(property.value)) {
      var simplifiedChild = simplifyObject(property.value, t);

      if (!t.isStringLiteral(simplifiedChild)) {
        bailout = true;
        return;
      }

      finalString += key + "{" + simplifiedChild.value + "}";
      return;
    }

    var value = property.value.value;
    finalString += serializeStyles({}, [(_ref = {}, _ref[key] = value, _ref)]).styles;
  });
  return bailout ? node : t.stringLiteral(finalString);
}

var appendStringToExpressions = function appendStringToExpressions(expressions, string, t) {
  if (!string) {
    return expressions;
  }

  if (t.isStringLiteral(expressions[expressions.length - 1])) {
    expressions[expressions.length - 1].value += string;
  } else {
    expressions.push(t.stringLiteral(string));
  }

  return expressions;
};
var joinStringLiterals = function joinStringLiterals(expressions, t) {
  return expressions.reduce(function (finalExpressions, currentExpression, i) {
    if (!t.isStringLiteral(currentExpression)) {
      finalExpressions.push(currentExpression);
    } else if (t.isStringLiteral(finalExpressions[finalExpressions.length - 1])) {
      finalExpressions[finalExpressions.length - 1].value += currentExpression.value;
    } else {
      finalExpressions.push(currentExpression);
    }

    return finalExpressions;
  }, []);
};

export { appendStringToExpressions, joinStringLiterals, getExpressionsFromTemplateLiteral, getLabelFromPath, getSourceMap, getTargetClassName, simplifyObject };
