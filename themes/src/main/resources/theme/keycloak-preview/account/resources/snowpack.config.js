const postcss = require('rollup-plugin-postcss');

module.exports = {
  "webDependencies": [
    "@patternfly/react-core",
    "@patternfly/react-icons",
    "@patternfly/react-styles",
    "@patternfly/react-tokens",
    // "querystring", <-- snowpack errors
    // "url", <-- snowpack errors
    "object-keys",
    "punycode",
    "react",
    "react-dom",
    "react-router-dom"
  ],
  rollup: {
    plugins: [postcss()]
  }
};