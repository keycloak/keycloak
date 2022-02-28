/**
 * Adds `.jsx`, `.ts` and `.tsx` as an extension, and enables JSX/TSX parsing.
 */
var jsExtensions = ['.js', '.jsx'];
var tsExtensions = ['.ts', '.tsx'];
var allExtensions = jsExtensions.concat(tsExtensions);

module.exports = {

  settings: {
    'import/extensions': allExtensions,
    'import/parsers': {
      'typescript-eslint-parser': tsExtensions
    },
    'import/resolver': {
      'node': {
        'extensions': allExtensions
      }
    }
  }

}
