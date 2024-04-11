import path from "node:path";

const CONFIG_PATH = path.resolve(import.meta.dirname, "eslint.config.js");

export default {
  "*.{js,jsx,mjs,ts,tsx}": (filenames) =>
    `eslint --config ${CONFIG_PATH} --cache --fix ${filenames.join(" ")}`,
};
