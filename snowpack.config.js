module.exports = {
  extends: "@snowpack/app-scripts-react",
  proxy: {
    "/admin": process.env.BACKEND_URL,
  },
  plugins: ["@snowpack/plugin-postcss", "@snowpack/plugin-webpack"],
};
