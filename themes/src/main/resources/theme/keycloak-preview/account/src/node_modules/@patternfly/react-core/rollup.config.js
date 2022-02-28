import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import scss from 'rollup-plugin-scss';
import replace from '@rollup/plugin-replace';
import { terser } from 'rollup-plugin-terser';

const isProduction = process.env.IS_PRODUCTION;

module.exports = {
  input: 'dist/esm/index.js',
  output: {
    file: `dist/umd/react-core${isProduction ? '.min' : ''}.js`,
    format: 'umd',
    name: 'PatternFlyReact',
    globals: {
      react: 'React',
      'react-dom': 'ReactDOM',
      'prop-types': 'PropTypes'
    }
  },
  external: ['react', 'react-dom', 'prop-types'],
  plugins: [
    replace({
      'process.env.NODE_ENV': `'${isProduction ? 'production' : 'development'}'`
    }),
    resolve(),
    commonjs(),
    scss(),
    isProduction && terser()
  ]
};
