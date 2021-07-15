#!/usr/bin/env node
const http = require("https");
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");

const decompress = require("decompress");
const decompressTargz = require("decompress-targz");

const args = process.argv.slice(2);
const version = args[0] || "14.0.0";

const folder = "server";
const fileName = path.join(folder, `keycloak-${version}.tar.gz`);
const serverPath = path.join(folder, `keycloak-${version}`);
const extension = process.platform === "win32" ? ".bat" : ".sh";

if (!fs.existsSync(folder)) {
  fs.mkdirSync(folder);
}

const decompressKeycloak = () =>
  decompress(fileName, folder, {
    plugins: [decompressTargz()],
  })
    .then(() => {
      console.log("Files decompressed");
    })
    .catch((e) => console.error(e));

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
