const copy = require('rollup-plugin-copy');
const postcss = require('rollup-plugin-postcss');

module.exports = {
  rollup: {
    plugins: [
      postcss({
        extract: 'public/app.css'
      }),
      copy({
        targets: [
          { src: 'node_modules/@patternfly/react-core/dist/styles/base.css', dest: 'public/' },
          { src: 'node_modules/@patternfly/react-core/dist/styles/assets/fonts/overpass-webfont/overpass*.woff2', dest: 'public/assets/fonts/overpass-webfont/'},
          { src: 'node_modules/@patternfly/react-core/dist/styles/assets/pficon/pficon.woff2', dest: 'public/assets/pficon/'},
        ],
      })
    ]
  }
};