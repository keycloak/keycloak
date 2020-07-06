import typescript from 'rollup-plugin-typescript2'
import commonjs from 'rollup-plugin-commonjs'
import external from 'rollup-plugin-peer-deps-external'
import resolve from 'rollup-plugin-node-resolve'
import postcss from 'rollup-plugin-postcss'

import pkg from './package.json'

export default {
  input: 'app/index.ts',
  output: [
    {
      file: pkg.main,
      format: 'cjs',
      exports: 'named',
      sourcemap: true
    },
    {
      file: pkg.module,
      format: 'es',
      exports: 'named',
      sourcemap: true
    }
  ],
  plugins: [
    external(),
    resolve(),
    postcss({
      extract: 'dist/index.css'
    }),
    typescript({
      rollupCommonJSResolveHack: true,
      exclude: [
        '**/__tests__/**'
      ],
      clean: true
    }),
    commonjs({
      include: ['node_modules/**']
    })
  ]
}
