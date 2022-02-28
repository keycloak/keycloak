const nodeResolve = require('rollup-plugin-node-resolve')
const commonjs = require('rollup-plugin-commonjs')
const babel = require('rollup-plugin-babel')
const uglify = require('rollup-plugin-uglify')

const umdGlobals = {
  react: 'React',
  'prop-types': 'PropTypes'
}

module.exports = [
  {
    input: './src/index.js',
    output: {
      file: 'dist/index.js',
      format: 'umd',
      name: 'Dropzone',
      globals: umdGlobals,
      sourcemap: 'inline'
    },
    external: Object.keys(umdGlobals),
    plugins: [
      nodeResolve(),
      commonjs({ include: '**/node_modules/**' }),
      babel({ exclude: '**/node_modules/**', plugins: ['external-helpers'] }),
      uglify()
    ]
  }
]
