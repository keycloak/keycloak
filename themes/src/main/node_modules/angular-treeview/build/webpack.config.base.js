/**
 * Created by hyku on 16/9/29.
 */
var path = require("path");
var webpack = require("webpack");

var config = {
    entry: {
        tree: "./src/tree.js"
    },
    output: {
        path: path.resolve(__dirname, ".."),
        filename: "[name].js"
    },
    // resolve: {
    //     root: path.resolve(__dirname, "./src")
    // },
    module: {
        loaders: [
            {
                test: /\.less$/i,
                loaders: ["style", "css", "less"]
            },
            {
                test: /\.html$/i,
                loaders: ["html"]
            },
            {
                test: /\.css$/i,
                loaders: ["style", "css"]
            },
            {
                test: /\.(jpe?g|png|gif|svg)$/i,
                loader: "url-loader?limit=10000&name=images/[name].[ext]"
            },
            {
                test: /\.(ttf|eot|woff2?)$/,
                loader: 'file?name=etc/[name].[ext]'
            }
        ]
    },
    plugins: [
        new webpack.optimize.DedupePlugin(),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        }),
        new webpack.optimize.OccurenceOrderPlugin()
    ]
};

module.exports = config;
