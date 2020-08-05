module.exports = {
  "extends": "@snowpack/app-scripts-react",
  "scripts": {
    "build:css": "postcss"
  },
  "proxy": {
    "/realms": process.env.BACKEND_URL
  },
  "plugins": ["@snowpack/plugin-parcel"]

}
