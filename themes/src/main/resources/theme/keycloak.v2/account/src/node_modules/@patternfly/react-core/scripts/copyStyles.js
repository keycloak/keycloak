/* eslint-disable @typescript-eslint/no-var-requires */
const { copySync, readFileSync, writeFileSync } = require('fs-extra');
const { resolve, dirname, join } = require('path');
const { parse: parseCSS, stringify: stringifyCSS } = require('css');
/* eslint-enable @typescript-eslint/no-var-requires */

const stylesDir = resolve(__dirname, '../dist/styles');
const pfDir = dirname(require.resolve('@patternfly/patternfly/patternfly.css'));

const unusedSelectorRegEx = /(\.fas?|\.sr-only)/;
const unusedKeyFramesRegEx = /fa-/;
const unusedFontFamilyRegEx = /Font Awesome 5 Free/;
const ununsedFontFilesRegExt = /(fa-|\.html$|\.css$)/;

// Copy assets
copySync(join(pfDir, 'assets/images'), join(stylesDir, 'assets/images'));
copySync(join(pfDir, 'assets/pficon'), join(stylesDir, 'assets/pficon'));
copySync(join(pfDir, 'assets/fonts'), join(stylesDir, 'assets/fonts'), {
  filter(src) {
    return !ununsedFontFilesRegExt.test(src);
  }
});

// Copy css
const baseCssFiles = {
  'base.css': 'patternfly-base.css',
  'base-no-reset.css': 'patternfly-base-no-reset.css'
};

for (const [targetCss, baseCss] of Object.entries(baseCssFiles)) {
  const css = readFileSync(join(pfDir, baseCss), 'utf8');
  const ast = parseCSS(css);

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

  writeFileSync(join(stylesDir, targetCss), stringifyCSS(ast));
}
