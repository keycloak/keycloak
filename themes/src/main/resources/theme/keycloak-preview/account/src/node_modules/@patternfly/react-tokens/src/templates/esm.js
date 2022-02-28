const { join } = require('path');

module.exports = {
  getOutputPath: ({ outDir }) => join(outDir, 'esm/index.js'),
  getContent: ({ tokens }) =>
    Object.keys(tokens).reduce((acc, key) => `${acc}export const ${key} = ${JSON.stringify(tokens[key])}\n`, ''),
  getSingleOutputPath: ({ outDir, tokenName }) => join(outDir, `esm/${tokenName}.js`),
  getSingleContent: ({ tokenValue }) => `export default ${JSON.stringify(tokenValue)}\n`
};
