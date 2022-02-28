/* eslint-disable @typescript-eslint/no-var-requires */
const { copySync, readFileSync, writeFileSync } = require('fs-extra');
const { resolve, dirname, join } = require('path');
const { parse: parseCSS, stringify: stringifyCSS } = require('css');
/* eslint-enable @typescript-eslint/no-var-requires */

const baseCSSFilename = 'patternfly-base.css';
const stylesDir = resolve(__dirname, '../dist/styles');
const pfDir = dirname(require.resolve(`@patternfly/patternfly/${baseCSSFilename}`));

const css = readFileSync(join(pfDir, baseCSSFilename), 'utf8');
const ast = parseCSS(css);

const unusedSelectorRegEx = /(\.fas?|\.sr-only)/;
const unusedKeyFramesRegEx = /fa-/;
const unusedFontFamilyRegEx = /Font Awesome 5 Free/;
const ununsedFontFilesRegExt = /(fa-|\.html$|\.css$)/;

// Core provides font awesome fonts and utlities. React does not use these
ast.stylesheet.rules = ast.stylesheet.rules.filter(rule => {
  switch (rule.type) {
    case 'rule':
      return !rule.selectors.some(sel => unusedSelectorRegEx.test(sel));
    case 'keyframes':
      return !unusedKeyFramesRegEx.test(rule.name);
    case 'charset':
    case 'comment':
      return false;
    case 'font-face':
      // eslint-disable-next-line no-case-declarations
      const fontFamilyDecl = rule.declarations.find(decl => decl.property === 'font-family');
      return !unusedFontFamilyRegEx.test(fontFamilyDecl.value);
    default:
      return true;
  }
});

copySync(join(pfDir, 'assets/images'), join(stylesDir, 'assets/images'));
copySync(join(pfDir, 'assets/pficon'), join(stylesDir, 'assets/pficon'));
copySync(join(pfDir, 'assets/fonts'), join(stylesDir, 'assets/fonts'), {
  filter(src) {
    return !ununsedFontFilesRegExt.test(src);
  }
});
writeFileSync(join(stylesDir, 'base.css'), stringifyCSS(ast));
