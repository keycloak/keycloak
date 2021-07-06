// See: https://github.com/snowpackjs/snowpack/issues/3242
// @ts-ignore
import snowpackConfig from "@snowpack/app-scripts-react/jest.config.js";
import type { Config } from "@jest/types";

const config: Config.InitialOptions = {
  ...snowpackConfig(),
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  moduleNameMapper: {
    "\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$": "<rootDir>/src/__mocks__/fileMock.js",
    "\\.(css|less)$": "<rootDir>/src/__mocks__/styleMock.js"
  }
};

export default config;
