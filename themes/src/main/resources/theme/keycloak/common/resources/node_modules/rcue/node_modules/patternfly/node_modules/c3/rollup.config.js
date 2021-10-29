import babel from 'rollup-plugin-babel';
import pkg from './package.json'

export default {
    input: 'src/index.js',
    output: {
        name: 'c3',
        format: 'umd',
        banner: `/* @license C3.js v${pkg.version} | (c) C3 Team and other contributors | http://c3js.org/ */`
    },
    plugins: [babel({
        presets: [['es2015', {
            modules: false
        }]],
        plugins: [
            'external-helpers'
        ]
    })]
};
