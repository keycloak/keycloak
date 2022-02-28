const { minify } = require("terser");

const transform = (code, optionsString) => {
  const options = eval(`(${optionsString})`);
  const result = minify(code, options);
  if (result.error) {
    throw result.error;
  } else {
    return { result, nameCache: options.nameCache };
  }
};

exports.transform = transform;
