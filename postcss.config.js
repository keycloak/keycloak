module.exports = {
  plugins: [
    require('postcss-import')({path: ['node_modules/@patternfly/patternfly/']}),
  ]
}