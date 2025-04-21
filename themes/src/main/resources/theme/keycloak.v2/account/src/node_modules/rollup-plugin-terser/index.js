const { codeFrameColumns } = require("@babel/code-frame");
const Worker = require("jest-worker").default;
const serialize = require("serialize-javascript");
const { createFilter } = require("rollup-pluginutils");

function terser(userOptions = {}) {
  if (userOptions.sourceMap != null) {
    throw Error("sourceMap option is removed, use sourcemap instead");
  }

  const filter = createFilter(userOptions.include, userOptions.exclude, {
    resolve: false
  });

  return {
    name: "terser",

    renderChunk(code, chunk, outputOptions) {
      if (!filter(chunk.fileName)) {
        return null;
      }

      if (!this.worker) {
        this.worker = new Worker(require.resolve("./transform.js"), {
          numWorkers: userOptions.numWorkers
        });
        this.numOfBundles = 0;
      }

      this.numOfBundles++;

      const defaultOptions = {
        sourceMap: userOptions.sourcemap !== false
      };
      if (outputOptions.format === "es" || outputOptions.format === "esm") {
        defaultOptions.module = true;
      }
      if (outputOptions.format === "cjs") {
        defaultOptions.toplevel = true;
      }

      // TODO rewrite with object spread after dropping node v6
      const normalizedOptions = Object.assign({}, defaultOptions, userOptions);

      // remove plugin specific options
      for (let key of ["include", "exclude", "sourcemap", "numWorkers"]) {
        if (normalizedOptions.hasOwnProperty(key)) {
          delete normalizedOptions[key];
        }
      }

      const serializedOptions = serialize(normalizedOptions);

      const result = this.worker
        .transform(code, serializedOptions)
        .catch(error => {
          const { message, line, col: column } = error;
          console.error(
            codeFrameColumns(code, { start: { line, column } }, { message })
          );
          throw error;
        });

      const handler = () => {
        this.numOfBundles--;

        if (this.numOfBundles === 0) {
          this.worker.end();
          this.worker = 0;
        }
      };

      result.then(handler, handler);

      return result.then(result => {
        if (result.nameCache) {
          let { vars, props } = userOptions.nameCache;

          // only assign nameCache.vars if it was provided, and if terser produced values:
          if (vars) {
            const newVars =
              result.nameCache.vars && result.nameCache.vars.props;
            if (newVars) {
              vars.props = vars.props || {};
              Object.assign(vars.props, newVars);
            }
          }

          // support populating an empty nameCache object:
          if (!props) {
            props = userOptions.nameCache.props = {};
          }

          // merge updated props into original nameCache object:
          const newProps =
            result.nameCache.props && result.nameCache.props.props;
          if (newProps) {
            props.props = props.props || {};
            Object.assign(props.props, newProps);
          }
        }

        return result.result;
      });
    }
  };
}

exports.terser = terser;
