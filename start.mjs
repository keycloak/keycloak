#!/usr/bin/env node
import http from "node:https";
import fs from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";

import decompress from "decompress";
import decompressTargz from "decompress-targz";

const args = process.argv.slice(2);
const version = args[0] || "15.0.1";

const folder = "server";
const fileName = path.join(folder, `keycloak-${version}.tar.gz`);
const serverPath = path.join(folder, `keycloak-${version}`);
const extension = process.platform === "win32" ? ".bat" : ".sh";

if (!fs.existsSync(folder)) {
  fs.mkdirSync(folder);
}

async function decompressKeycloak() {
  try {
    await decompress(fileName, folder, {
      plugins: [decompressTargz()],
    });

    console.log("Files decompressed");
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
      "-Dprofile.feature.newadmin=enabled",
    ]);
    proc.stdout.on("data", (data) => {
      console.log(data.toString());
    });
  });
};

const request = (url, file) => {
  http.get(url, (response) => {
    if (response.statusCode == 302) {
      request(response.headers.location, file);
    } else {
      response.pipe(file);
      response.on("end", () => {
        console.log("Downloaded keycloak");
        decompressKeycloak().then(() => run());
      });
    }
  });
};

if (!fs.existsSync(fileName)) {
  const file = fs.createWriteStream(fileName);
  request(
    `https://github.com/keycloak/keycloak/releases/download/${version}/keycloak-${version}.tar.gz`,
    file
  );
} else if (!fs.existsSync(serverPath)) {
  decompressKeycloak().then(() => run());
} else {
  run();
}
