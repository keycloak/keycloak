import babel from 'rollup-plugin-babel';

export default {
    entry: 'src/index.js',
    format: 'umd',
    moduleName: 'c3',
    plugins: [babel({
        presets: [['es2015', {
            modules: false
        }]],
        plugins: [
            'external-helpers'
        ]
    })]
};
