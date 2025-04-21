const { outputFileSync } = require('fs-extra');
const { resolve, join } = require('path');
const { generateTokens } = require('./generateTokens');

const outDir = resolve(__dirname, '../dist');

const writeESMExport = (tokenName, tokenString) =>
  outputFileSync(
    join(outDir, 'esm/', `${tokenName}.js`),
    `
export const ${tokenName} = ${tokenString};
export default ${tokenName};
`.trim()
  );

const writeCJSExport = (tokenName, tokenString) =>
  outputFileSync(
    join(outDir, 'js', `${tokenName}.js`),
    `
"use strict";
exports.__esModule = true;
exports.${tokenName} = ${tokenString};
exports["default"] = exports.${tokenName};
`.trim()
  );

const writeDTSExport = (tokenName, tokenString) => {
  const text = `
export const ${tokenName}: ${tokenString};
export default ${tokenName};
`.trim();
  const filename = `${tokenName}.d.ts`;
  outputFileSync(join(outDir, 'esm', filename), text);
  outputFileSync(join(outDir, 'js', filename), text);
};

const allIndex = {};
const componentIndex = [];

const outputIndex = (index, indexFile) => {
  const esmIndexString = index.map(file => `export { ${file} } from './${file}';`).join('\n');
  outputFileSync(join(outDir, 'esm', indexFile), esmIndexString);
  outputFileSync(
    join(outDir, 'js', indexFile),
    `
"use strict";
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
exports.__esModule = true;
${index.map(file => `__export(require('./${file}'));`).join('\n')}
`.trim()
  );
  outputFileSync(join(outDir, 'esm', indexFile.replace('.js', '.d.ts')), esmIndexString);
  outputFileSync(join(outDir, 'js', indexFile.replace('.js', '.d.ts')), esmIndexString);
};

/**
 * Writes CJS and ESM tokens to `dist` directory
 *
 * @param {any} tokens tokens from generateTokens
 */
function writeTokens(tokens) {
  Object.entries(tokens).forEach(([tokenName, tokenValue]) => {
    const tokenString = JSON.stringify(tokenValue, null, 2);

    writeESMExport(tokenName, tokenString);
    writeCJSExport(tokenName, tokenString);
    writeDTSExport(tokenName, tokenString);
    allIndex[tokenName] = true;
    componentIndex.push(tokenName);

    // Legacy token support -- values may be incorrect.
    Object.values(tokenValue)
      .map(values => Object.entries(values))
      .reduce((acc, val) => acc.concat(val), []) // flatten
      .forEach(([oldTokenName, { name, value }]) => {
        const isChart = oldTokenName.includes('chart');
        const oldToken = {
          name,
          value: isChart && !isNaN(+value) ? +value : value,
          var: isChart ? `var(${name}, ${value})` : `var(${name})` // Include fallback value for chart vars
        };
        const oldTokenString = JSON.stringify(oldToken, null, 2);
        writeESMExport(oldTokenName, oldTokenString);
        writeCJSExport(oldTokenName, oldTokenString);
        writeDTSExport(oldTokenName, oldTokenString);
        allIndex[oldTokenName] = true;
      });
  });

  // Index files including legacy tokens
  outputIndex(Object.keys(allIndex), 'index.js');
  outputIndex(componentIndex, 'componentIndex.js');

  // eslint-disable-next-line no-console
  console.log('Wrote', Object.keys(allIndex).length * 4 + 4, 'token files');
}

writeTokens(generateTokens());
