#!/usr/bin/env node
import http from "node:https";
import fs from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";

import decompress from "decompress";

import ProgressPromise from "progress-promise";
import cliProgress from "cli-progress";
import colors from "colors";

const args = process.argv.slice(2);
const version = args[0] && !args[0].startsWith("-") ? args[0] : "16.1.0";

const folder = "server";
const fileName = path.join(folder, `keycloak-${version}.tar.gz`);
const serverPath = path.join(folder, `keycloak-${version}`);
const extension = process.platform === "win32" ? ".bat" : ".sh";

if (!fs.existsSync(folder)) {
  fs.mkdirSync(folder);
}

const progressTick = () => {
  return new ProgressPromise((resolve, _, progress) => {
    for (let i = 0; i < 10; i++) {
      setTimeout(() => progress(i * 10), i * 1500);
    }
    setTimeout(resolve, 15000);
  });
};

async function decompressKeycloak() {
  try {
    const progressBar = new cliProgress.Bar({
      format:
        "Decompress |" +
        colors.cyan("{bar}") +
        "| {percentage}% || ETA: {eta}s ",
      barCompleteChar: "\u2588",
      barIncompleteChar: "\u2591",
    });
    progressBar.start(100);
    await Promise.all([
      decompress(fileName, folder),
      progressTick()
        .progress((value) => progressBar.update(value))
        .then(() => {
          progressBar.update(100);
          progressBar.stop();
          console.log("\nFiles decompressed");
        }),
    ]);
  } catch (error) {
    console.error(error);
  }
}

const run = () => {
  const addProc = spawn(
    path.join(serverPath, "bin", `add-user-keycloak${extension}`),
    ["--user", "admin", "--password", "admin"]
  );

  addProc.on("exit", () => {
    const proc = spawn(path.join(serverPath, "bin", `standalone${extension}`), [
      "-Djboss.socket.binding.port-offset=100",
      "-Dkeycloak.profile.feature.admin2=enabled",
      "-Dkeycloak.profile.feature.declarative_user_profile=enabled",
      ...args,
    ]);
    proc.stdout.on("data", (data) => {
      console.log(data.toString());
    });
  });
};

const request = (url, file, progressBar) => {
  http.get(url, (response) => {
    if (response.statusCode === 302) {
      request(response.headers.location, file, progressBar);
    } else if (response.statusCode === 404) {
      throw new Error(`version not found '${version}'`);
    } else {
      let data = 0;
      progressBar.start(parseInt(response.headers["content-length"]), 0);
      response.pipe(file);
      response.on("data", (chunk) => {
        progressBar.update(data);
        data += chunk.length;
      });
      response.on("end", () => {
        console.log("\nDownloaded keycloak");
        decompressKeycloak().then(() => run());
      });
    }
  });
};

if (!fs.existsSync(fileName)) {
  const file = fs.createWriteStream(fileName);
  const progressBar = new cliProgress.Bar({
    format:
      "Download |" +
      colors.cyan("{bar}") +
      "| {percentage}% || {value}/{total} Chunks",
    barCompleteChar: "\u2588",
    barIncompleteChar: "\u2591",
  });

  request(
    `https://github.com/keycloak/keycloak/releases/download/${version}/keycloak-${version}.tar.gz`,
    file,
    progressBar
  );
} else if (!fs.existsSync(serverPath)) {
  decompressKeycloak().then(() => run());
} else {
  run();
}
