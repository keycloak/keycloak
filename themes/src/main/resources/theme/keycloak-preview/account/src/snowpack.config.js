const copy = require('rollup-plugin-copy');
const postcss = require('rollup-plugin-postcss');

const appDest = '../resources/'
const appDestPublic = appDest + 'public/'

module.exports = {
  rollup: {
    plugins: [
      postcss({
        extract: appDestPublic + 'app.css'
      }),
      copy({
        targets: [
          { src: 'node_modules/@patternfly/react-core/dist/styles/base.css', dest: appDestPublic },
          { src: 'node_modules/@patternfly/react-core/dist/styles/assets/fonts/overpass-webfont/overpass*.woff2', dest: appDestPublic + 'assets/fonts/overpass-webfont/'},
          { src: 'node_modules/@patternfly/react-core/dist/styles/assets/pficon/pficon.woff2', dest: appDestPublic + 'assets/pficon/'},
        ],
      })
    ]
  }
};