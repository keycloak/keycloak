"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.addSourceMappingUrl = addSourceMappingUrl;
exports.chmod = chmod;
exports.compile = compile;
exports.debounce = debounce;
exports.deleteDir = deleteDir;
exports.isCompilableExtension = isCompilableExtension;
exports.readdir = readdir;
exports.readdirForCompilable = readdirForCompilable;
exports.transformRepl = transformRepl;
exports.withExtension = withExtension;

function _fsReaddirRecursive() {
  const data = require("fs-readdir-recursive");

  _fsReaddirRecursive = function () {
    return data;
  };

  return data;
}

function babel() {
  const data = require("@babel/core");

  babel = function () {
    return data;
  };

  return data;
}

function _path() {
  const data = require("path");

  _path = function () {
    return data;
  };

  return data;
}

function _fs() {
  const data = require("fs");

  _fs = function () {
    return data;
  };

  return data;
}

var watcher = require("./watcher");

function asyncGeneratorStep(gen, resolve, reject, _next, _throw, key, arg) { try { var info = gen[key](arg); var value = info.value; } catch (error) { reject(error); return; } if (info.done) { resolve(value); } else { Promise.resolve(value).then(_next, _throw); } }

function _asyncToGenerator(fn) { return function () { var self = this, args = arguments; return new Promise(function (resolve, reject) { var gen = fn.apply(self, args); function _next(value) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "next", value); } function _throw(err) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "throw", err); } _next(undefined); }); }; }

function chmod(src, dest) {
  try {
    _fs().chmodSync(dest, _fs().statSync(src).mode);
  } catch (err) {
    console.warn(`Cannot change permissions of ${dest}`);
  }
}

function readdir(dirname, includeDotfiles, filter) {
  return _fsReaddirRecursive()(dirname, (filename, _index, currentDirectory) => {
    const stat = _fs().statSync(_path().join(currentDirectory, filename));

    if (stat.isDirectory()) return true;
    return (includeDotfiles || filename[0] !== ".") && (!filter || filter(filename));
  });
}

function readdirForCompilable(dirname, includeDotfiles, altExts) {
  return readdir(dirname, includeDotfiles, function (filename) {
    return isCompilableExtension(filename, altExts);
  });
}

function isCompilableExtension(filename, altExts) {
  const exts = altExts || babel().DEFAULT_EXTENSIONS;

  const ext = _path().extname(filename);

  return exts.includes(ext);
}

function addSourceMappingUrl(code, loc) {
  return code + "\n//# sourceMappingURL=" + _path().basename(loc);
}

const CALLER = {
  name: "@babel/cli"
};

function transformRepl(filename, code, opts) {
  opts = Object.assign({}, opts, {
    caller: CALLER,
    filename
  });
  return new Promise((resolve, reject) => {
    babel().transform(code, opts, (err, result) => {
      if (err) reject(err);else resolve(result);
    });
  });
}

function compile(_x, _x2) {
  return _compile.apply(this, arguments);
}

function _compile() {
  _compile = _asyncToGenerator(function* (filename, opts) {
    opts = Object.assign({}, opts, {
      caller: CALLER
    });
    const result = yield new Promise((resolve, reject) => {
      babel().transformFile(filename, opts, (err, result) => {
        if (err) reject(err);else resolve(result);
      });
    });

    if (result) {
      {
        if (!result.externalDependencies) return result;
      }
      watcher.updateExternalDependencies(filename, result.externalDependencies);
    }

    return result;
  });
  return _compile.apply(this, arguments);
}

function deleteDir(path) {
  if (_fs().existsSync(path)) {
    _fs().readdirSync(path).forEach(function (file) {
      const curPath = path + "/" + file;

      if (_fs().lstatSync(curPath).isDirectory()) {
        deleteDir(curPath);
      } else {
        _fs().unlinkSync(curPath);
      }
    });

    _fs().rmdirSync(path);
  }
}

process.on("uncaughtException", function (err) {
  console.error(err);
  process.exitCode = 1;
});

function withExtension(filename, ext = ".js") {
  const newBasename = _path().basename(filename, _path().extname(filename)) + ext;
  return _path().join(_path().dirname(filename), newBasename);
}

function debounce(fn, time) {
  let timer;

  function debounced() {
    clearTimeout(timer);
    timer = setTimeout(fn, time);
  }

  debounced.flush = () => {
    clearTimeout(timer);
    fn();
  };

  return debounced;
}