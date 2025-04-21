const { join } = require('path');
const { outputFileSync } = require('fs-extra');
const { generateIcons } = require('./generateIcons');

const outDir = join(__dirname, '../dist');

const removeSnake = s =>
  s
    .toUpperCase()
    .replace('-', '')
    .replace('_', '');
const toCamel = s => `${s[0].toUpperCase()}${s.substr(1).replace(/([-_][\w])/gi, removeSnake)}`;

const writeCJSExport = (fname, jsName, icon) => {
  outputFileSync(
    join(outDir, 'js/icons', `${fname}.js`),
    `"use strict"
exports.__esModule = true;
exports.${jsName}Config = {
  name: '${jsName}',
  height: ${icon.height},
  width: ${icon.width},
  svgPath: '${icon.svgPathData}',
  yOffset: ${icon.yOffset || 0},
  xOffset: ${icon.xOffset || 0},
};
exports.${jsName} = require('../createIcon').createIcon(exports.${jsName}Config);
exports["default"] = exports.${jsName};
    `.trim()
  );
};

const writeESMExport = (fname, jsName, icon) => {
  outputFileSync(
    join(outDir, 'esm/icons', `${fname}.js`),
    `import { createIcon } from '../createIcon';

export const ${jsName}Config = {
  name: '${jsName}',
  height: ${icon.height},
  width: ${icon.width},
  svgPath: '${icon.svgPathData}',
  yOffset: ${icon.yOffset || 0},
  xOffset: ${icon.xOffset || 0},
};

export const ${jsName} = createIcon(${jsName}Config);

export default ${jsName};
    `.trim()
  );
};

const writeDTSExport = (fname, jsName, icon) => {
  const text = `import * as React from 'react';
import { SVGIconProps } from '../createIcon';
export declare const ${jsName}Config: {
  name: '${jsName}',
  height: ${icon.height},
  width: ${icon.width},
  svgPath: '${icon.svgPathData}',
  yOffset: ${icon.yOffset || 0},
  xOffset: ${icon.xOffset || 0},
};
export declare const ${jsName}: React.ComponentClass<SVGIconProps>;
export default ${jsName};
    `.trim();
  const filename = `${fname}.d.ts`;
  outputFileSync(join(outDir, 'js/icons', filename), text);
  outputFileSync(join(outDir, 'esm/icons', filename), text);
};

/**
 * Writes CJS and ESM icons to `dist` directory
 *
 * @param {any} icons icons from generateIcons
 */
function writeIcons(icons) {
  const index = [];
  Object.entries(icons).forEach(([iconName, icon]) => {
    const fname = `${iconName}-icon`;
    const jsName = `${toCamel(iconName)}Icon`;
    writeESMExport(fname, jsName, icon);
    writeCJSExport(fname, jsName, icon);
    writeDTSExport(fname, jsName, icon);

    index.push({ fname, jsName });
  });

  const esmIndexString = index
    .map(({ fname, jsName }) => `export { ${jsName}, ${jsName}Config } from './${fname}';`)
    .sort()
    .join('\n');
  outputFileSync(join(outDir, 'esm', 'icons/index.js'), esmIndexString);
  outputFileSync(join(outDir, 'esm', 'icons/index.d.ts'), esmIndexString);
  outputFileSync(join(outDir, 'js', 'icons/index.d.ts'), esmIndexString);
  outputFileSync(
    join(outDir, 'js', 'icons/index.js'),
    `"use strict";
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
exports.__esModule = true;
${index
  .map(({ fname }) => `__export(require('./${fname}'));`)
  .sort()
  .join('\n')}
`.trim()
  );

  // eslint-disable-next-line no-console
  console.log('Wrote', index.length * 3 + 3, 'icon files.');
}

writeIcons(generateIcons());
