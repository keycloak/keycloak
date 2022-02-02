import type { InitialOptionsTsJest } from "ts-jest";

const config: InitialOptionsTsJest = {
  preset: "ts-jest",
  globals: {
    "ts-jest": {
      tsconfig: "tsconfig.jest.json",
    },
  },
  testMatch: ["<rootDir>/src/**/*.test.ts?(x)"],
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  moduleNameMapper: {
    "\\.css$": "<rootDir>/mocks/fileMock.ts",
    "lodash-es": "lodash",
  },
};

export default config;
