"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = _default;

function _slash() {
  const data = require("slash");

  _slash = function () {
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

var util = require("./util");

var watcher = require("./watcher");

function asyncGeneratorStep(gen, resolve, reject, _next, _throw, key, arg) { try { var info = gen[key](arg); var value = info.value; } catch (error) { reject(error); return; } if (info.done) { resolve(value); } else { Promise.resolve(value).then(_next, _throw); } }

function _asyncToGenerator(fn) { return function () { var self = this, args = arguments; return new Promise(function (resolve, reject) { var gen = fn.apply(self, args); function _next(value) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "next", value); } function _throw(err) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "throw", err); } _next(undefined); }); }; }

const FILE_TYPE = Object.freeze({
  NON_COMPILABLE: "NON_COMPILABLE",
  COMPILED: "COMPILED",
  IGNORED: "IGNORED",
  ERR_COMPILATION: "ERR_COMPILATION"
});

function outputFileSync(filePath, data) {
  (((v, w) => (v = v.split("."), w = w.split("."), +v[0] > +w[0] || v[0] == w[0] && +v[1] >= +w[1]))(process.versions.node, "10.12") ? _fs().mkdirSync : require("make-dir").sync)(_path().dirname(filePath), {
    recursive: true
  });

  _fs().writeFileSync(filePath, data);
}

function _default(_x) {
  return _ref.apply(this, arguments);
}

function _ref() {
  _ref = _asyncToGenerator(function* ({
    cliOptions,
    babelOptions
  }) {
    function write(_x2, _x3) {
      return _write.apply(this, arguments);
    }

    function _write() {
      _write = _asyncToGenerator(function* (src, base) {
        let relative = _path().relative(base, src);

        if (!util.isCompilableExtension(relative, cliOptions.extensions)) {
          return FILE_TYPE.NON_COMPILABLE;
        }

        relative = util.withExtension(relative, cliOptions.keepFileExtension ? _path().extname(relative) : cliOptions.outFileExtension);
        const dest = getDest(relative, base);

        try {
          const res = yield util.compile(src, Object.assign({}, babelOptions, {
            sourceFileName: _slash()(_path().relative(dest + "/..", src))
          }));
          if (!res) return FILE_TYPE.IGNORED;

          if (res.map && babelOptions.sourceMaps && babelOptions.sourceMaps !== "inline") {
            const mapLoc = dest + ".map";
            res.code = util.addSourceMappingUrl(res.code, mapLoc);
            res.map.file = _path().basename(relative);
            outputFileSync(mapLoc, JSON.stringify(res.map));
          }

          outputFileSync(dest, res.code);
          util.chmod(src, dest);

          if (cliOptions.verbose) {
            console.log(_path().relative(process.cwd(), src) + " -> " + dest);
          }

          return FILE_TYPE.COMPILED;
        } catch (err) {
          if (cliOptions.watch) {
            console.error(err);
            return FILE_TYPE.ERR_COMPILATION;
          }

          throw err;
        }
      });
      return _write.apply(this, arguments);
    }

    function getDest(filename, base) {
      if (cliOptions.relative) {
        return _path().join(base, cliOptions.outDir, filename);
      }

      return _path().join(cliOptions.outDir, filename);
    }

    function handleFile(_x4, _x5) {
      return _handleFile.apply(this, arguments);
    }

    function _handleFile() {
      _handleFile = _asyncToGenerator(function* (src, base) {
        const written = yield write(src, base);

        if (cliOptions.copyFiles && written === FILE_TYPE.NON_COMPILABLE || cliOptions.copyIgnored && written === FILE_TYPE.IGNORED) {
          const filename = _path().relative(base, src);

          const dest = getDest(filename, base);
          outputFileSync(dest, _fs().readFileSync(src));
          util.chmod(src, dest);
        }

        return written === FILE_TYPE.COMPILED;
      });
      return _handleFile.apply(this, arguments);
    }

    function handle(_x6) {
      return _handle.apply(this, arguments);
    }

    function _handle() {
      _handle = _asyncToGenerator(function* (filenameOrDir) {
        if (!_fs().existsSync(filenameOrDir)) return 0;

        const stat = _fs().statSync(filenameOrDir);

        if (stat.isDirectory()) {
          const dirname = filenameOrDir;
          let count = 0;
          const files = util.readdir(dirname, cliOptions.includeDotfiles);

          for (const filename of files) {
            const src = _path().join(dirname, filename);

            const written = yield handleFile(src, dirname);
            if (written) count += 1;
          }

          return count;
        } else {
          const filename = filenameOrDir;
          const written = yield handleFile(filename, _path().dirname(filename));
          return written ? 1 : 0;
        }
      });
      return _handle.apply(this, arguments);
    }

    let compiledFiles = 0;
    let startTime = null;
    const logSuccess = util.debounce(function () {
      if (startTime === null) {
        return;
      }

      const diff = process.hrtime(startTime);
      console.log(`Successfully compiled ${compiledFiles} ${compiledFiles !== 1 ? "files" : "file"} with Babel (${diff[0] * 1e3 + Math.round(diff[1] / 1e6)}ms).`);
      compiledFiles = 0;
      startTime = null;
    }, 100);
    if (cliOptions.watch) watcher.enable({
      enableGlobbing: true
    });

    if (!cliOptions.skipInitialBuild) {
      if (cliOptions.deleteDirOnStart) {
        util.deleteDir(cliOptions.outDir);
      }

      (((v, w) => (v = v.split("."), w = w.split("."), +v[0] > +w[0] || v[0] == w[0] && +v[1] >= +w[1]))(process.versions.node, "10.12") ? _fs().mkdirSync : require("make-dir").sync)(cliOptions.outDir, {
        recursive: true
      });
      startTime = process.hrtime();

      for (const filename of cliOptions.filenames) {
        compiledFiles += yield handle(filename);
      }

      if (!cliOptions.quiet) {
        logSuccess();
        logSuccess.flush();
      }
    }

    if (cliOptions.watch) {
      let processing = 0;
      const {
        filenames
      } = cliOptions;
      let getBase;

      if (filenames.length === 1) {
        const base = filenames[0];

        const absoluteBase = _path().resolve(base);

        getBase = filename => {
          return filename === absoluteBase ? _path().dirname(base) : base;
        };
      } else {
        const filenameToBaseMap = new Map(filenames.map(filename => {
          const absoluteFilename = _path().resolve(filename);

          return [absoluteFilename, _path().dirname(filename)];
        }));
        const absoluteFilenames = new Map(filenames.map(filename => {
          const absoluteFilename = _path().resolve(filename);

          return [absoluteFilename, filename];
        }));

        const {
          sep
        } = _path();

        getBase = filename => {
          const base = filenameToBaseMap.get(filename);

          if (base !== undefined) {
            return base;
          }

          for (const [absoluteFilenameOrDir, relative] of absoluteFilenames) {
            if (filename.startsWith(absoluteFilenameOrDir + sep)) {
              filenameToBaseMap.set(filename, relative);
              return relative;
            }
          }

          return "";
        };
      }

      filenames.forEach(filenameOrDir => {
        watcher.watch(filenameOrDir);
      });
      watcher.startWatcher();
      watcher.onFilesChange(_asyncToGenerator(function* (filenames) {
        processing++;
        if (startTime === null) startTime = process.hrtime();

        try {
          const written = yield Promise.all(filenames.map(filename => handleFile(filename, getBase(filename))));
          compiledFiles += written.filter(Boolean).length;
        } catch (err) {
          console.error(err);
        }

        processing--;
        if (processing === 0 && !cliOptions.quiet) logSuccess();
      }));
    }
  });
  return _ref.apply(this, arguments);
}