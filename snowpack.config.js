module.exports = {
  extends: "@snowpack/app-scripts-react",
  proxy: {
    "/admin": "http://localhost:8180/auth/admin/",
  },
  plugins: ["@snowpack/plugin-postcss", "@snowpack/plugin-webpack"],
};
