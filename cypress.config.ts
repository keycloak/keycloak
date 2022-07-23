import { defineConfig } from "cypress";
import { deleteAsync } from "del";
import path from "node:path";
import { build, InlineConfig } from "vite";

export default defineConfig({
  projectId: "j4yhox",
  screenshotsFolder: "assets/screenshots",
  videosFolder: "assets/videos",
  chromeWebSecurity: false,
  viewportWidth: 1360,
  viewportHeight: 768,
  defaultCommandTimeout: 30000,
  videoCompression: false,
  numTestsKeptInMemory: 30,
  videoUploadOnPasses: false,
  retries: {
    runMode: 3,
  },
  e2e: {
    setupNodeEvents(on) {
      on("file:preprocessor", vitePreprocessor);
      on("after:spec", async (spec, results) => {
        if (!results.video) {
          return;
        }

        // Do we have failures for any retry attempts?
        const failures = results.tests.some(({ attempts }) =>
          attempts.some(({ state }) => state === "failed")
        );

        // Delete the video if the spec passed and no tests were retried.
        if (!failures) {
          await deleteAsync(results.video);
        }
      });
    },
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
  },
});

const cache: Record<string, string> = {};

// See https://adamlynch.com/preprocess-cypress-tests-with-vite/
async function vitePreprocessor(file: Cypress.FileObject) {
  const { filePath, outputPath, shouldWatch } = file;

  if (cache[filePath]) {
    return cache[filePath];
  }

  const filename = path.basename(outputPath);
  const filenameWithoutExtension = path.basename(
    outputPath,
    path.extname(outputPath)
  );

  const viteConfig: InlineConfig = {
    build: {
      emptyOutDir: false,
      minify: false,
      outDir: path.dirname(outputPath),
      sourcemap: true,
      write: true,
    },
  };

  if (filename.endsWith(".html")) {
    viteConfig.build!.rollupOptions = {
      input: {
        [filenameWithoutExtension]: filePath,
      },
    };
  } else {
    viteConfig.build!.lib = {
      entry: filePath,
      fileName: () => filename,
      formats: ["es"],
      name: filenameWithoutExtension,
    };
  }

  if (shouldWatch) {
    // @ts-ignore
    viteConfig.build.watch = true;
  }

  const watcher = await build(viteConfig);

  if ("on" in watcher) {
    watcher.on("event", (event) => {
      if (event.code === "END") {
        file.emit("rerun");
      }
    });
    file.on("close", () => {
      delete cache[filePath];
      watcher.close();
    });
  }

  return (cache[filePath] = outputPath);
}
