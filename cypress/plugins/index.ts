// The Webpack preprocessor does not include any types so it will have to be ignored.
// @ts-ignore
import webpackPreprocessor from "@cypress/webpack-batteries-included-preprocessor";
import ForkTsCheckerWebpackPlugin from "fork-ts-checker-webpack-plugin";
import path from "path";

// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

const configurePlugins: Cypress.PluginConfig = (on) => {
  const defaultOptions = webpackPreprocessor.defaultOptions.webpackOptions;
  const webpackOptions = {
    ...defaultOptions,
    context: path.resolve(__dirname, ".."),
    plugins: [
      new ForkTsCheckerWebpackPlugin({
        async: false,
      }),
    ],
  };

  on(
    "file:preprocessor",
    webpackPreprocessor({
      typescript: require.resolve("typescript"),
      webpackOptions,
    })
  );
};

export default configurePlugins;
