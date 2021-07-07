module.exports = {
  core: {
    builder: "webpack5"
  },
  stories: [
    "../src/**/*.stories.mdx",
    "../src/**/*.stories.@(js|jsx|ts|tsx)"
  ],
  addons: [
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    {
      name: "@storybook/addon-postcss",
      options: {
        // Explicitly enable PostCSS 8+ (see: https://storybook.js.org/addons/@storybook/addon-postcss)
        postcssLoaderOptions: {
          implementation: require("postcss"),
        },
      },
    },
  ]
}