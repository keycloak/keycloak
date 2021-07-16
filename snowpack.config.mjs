import proxy from "http2-proxy";
import path from "node:path";

const themeName = process.env.THEME_NAME ?? "keycloak";
const themePath = path.join("themes", themeName);

/** @type {import("snowpack").SnowpackUserConfig } */
export default {
  mount: {
    [themePath]: { url: "/", static: true },
    public: { url: "/", static: true },
    src: { url: "/" },
    "node_modules/@patternfly/patternfly/assets/fonts": {
      url: "/assets/fonts",
      static: true,
    },
    "node_modules/@patternfly/patternfly/assets/pficon": {
      url: "/assets/pficon",
      static: true,
    },
    "node_modules/@patternfly/patternfly/assets/images": {
      url: "/assets/images",
      static: true,
    },
  },
  plugins: [
    "@snowpack/plugin-postcss",
    "@snowpack/plugin-react-refresh",
    "@snowpack/plugin-typescript",
  ],
  routes: [
    {
      src: "/auth/admin/.*",
      dest: (req, res) =>
        proxy.web(req, res, {
          hostname: "localhost",
          port: 8180,
        }),
    },
  ],
  optimize: {
    bundle: true,
  },
  devOptions: {
    hmrErrorOverlay: false,
  },
};
