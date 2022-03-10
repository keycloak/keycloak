const postImport = require("postcss-import");
const postcssSVG = require("postcss-svg");

module.exports = {
  plugins: [postImport(), postcssSVG()],
};
