const postcss = require('rollup-plugin-postcss');

const appDest = '../resources/'
const appDestPublic = appDest + 'public/'

module.exports = {
  rollup: {
    plugins: [
      postcss({
        extract: appDestPublic + 'app.css'
      })
    ]
  }
};