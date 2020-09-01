const postImport = require('postcss-import');
module.exports = {
  plugins: [
    postImport({path: ['./node_modules/@patternfly/patternfly/']}),
  ]
}