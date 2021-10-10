const postcss = require('rollup-plugin-postcss');

module.exports = {
  rollup: {
    plugins: [
      postcss({
        extract: 'app.css'
      })
    ]
  }
};