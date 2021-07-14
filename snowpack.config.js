module.exports = {
  extends: "@snowpack/app-scripts-react",
  proxy: {
    "/auth/admin": "http://localhost:8180/auth/admin/",
  },
  plugins: ["@snowpack/plugin-postcss", "@snowpack/plugin-typescript"],
  buildOptions: {
    baseUrl: "/adminv2",
    clean: true,
  },
};
