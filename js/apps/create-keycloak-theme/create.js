#!/usr/bin/env node

import chalk from "chalk";
import { Command, InvalidArgumentError } from "commander";
import fs from "fs-extra";
import Mustache from "mustache";
import { mkdtemp } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { simpleGit } from "simple-git";
import { fileURLToPath } from "url";

const packageJson = JSON.parse(
  fs.readFileSync(
    join(fileURLToPath(import.meta.url), "..", "package.json"),
    "utf8",
  ),
);

function main() {
  new Command(packageJson.name)
    .version(packageJson.version)
    .description(packageJson.description)
    .arguments("<name>")
    .usage(`${chalk.green("<name>")} [options]`)
    .option(
      "-t, --type <name>",
      "the type of ui to be created either `account` or `admin` ",
      (value) => {
        if (value !== "account" && value !== "admin") {
          throw new InvalidArgumentError(
            "It should be either account or admin",
          );
        }
        return value;
      },
    )
    .action(async (name, options) => {
      const type = options.type || "account";
      console.log(
        `Creating a new ${chalk.green(name)} project of type ${chalk.green(type)}`,
      );

      await createProject(name, type);
      done(name);
    })
    .on("--help", () => {
      console.log();
      console.log(`    Only ${chalk.green("<name>")} is required.`);
      console.log();
      console.log(`    Type ${chalk.blue("--type")} can be one of:`);
      console.log(
        `      - ${chalk.green("account")} for an account ui based ui`,
      );
      console.log(`      - ${chalk.green("admin")} for an admin ui based ui`);
      console.log();
    })
    .parse(process.argv);
}

function cloneQuickstart(type) {
  return new Promise((resolve, reject) => {
    mkdtemp(join(tmpdir(), "template-"), async (err, dir) => {
      if (err) return reject(err);
      simpleGit()
        .clone("https://github.com/keycloak/keycloak-quickstarts", dir, {
          "--single-branch": undefined,
          "--branch": "main",
        })
        .then(() =>
          resolve(join(dir, `extension/extend-${type}-console-node`)),
        );
    });
  });
}

async function createProject(name, type) {
  const templateProjectDir = await cloneQuickstart(type);
  const projectDir = join(resolve(), name);
  await fs.mkdir(projectDir);
  await fs.copy(templateProjectDir, projectDir);
  const filename = fileURLToPath(import.meta.url);
  const templateDir = join(dirname(filename), "templates");
  const templateFiles = await searchFile(templateDir, "mu");
  templateFiles.forEach(async (file) => {
    const dest = file.substring(templateDir.length, file.length - 3);
    const destPath = join(projectDir, dest);
    const contents = await fs.readFile(file, "utf8");
    const data = Mustache.render(contents, {
      name,
      type,
      version: packageJson.version,
    });
    await fs.writeFile(destPath, data);
  });
}

async function searchFile(dir, fileName) {
  const result = [];
  const files = await fs.readdir(dir);

  for (const file of files) {
    const filePath = join(dir, file);
    const fileStat = await fs.stat(filePath);

    if (fileStat.isDirectory()) {
      result.push(...(await searchFile(filePath, fileName)));
    } else if (file.endsWith(fileName)) {
      result.push(filePath);
    }
  }
  return result;
}

function done(appName) {
  console.log();
  console.log(`Success! Created ${appName} at ./${appName}`);
  console.log("Inside that directory, you can run several commands:");
  console.log();
  console.log(chalk.cyan(`  npm run start-keycloak`));
  console.log("    Download and starts a keycloak server.");
  console.log();
  console.log(chalk.cyan(`  npm run dev`));
  console.log("    Starts development server.");
  console.log();
  console.log(chalk.cyan(`  mvn install`));
  console.log(
    "    Bundles the app into a jar file that can be deployed to a keycloak ",
  );
  console.log(
    `    server by putting it in the ${chalk.green("providers")} directory.`,
  );
  console.log();
  console.log("We suggest that you begin by typing:");
  console.log();
  console.log(chalk.cyan("  cd"), appName);
  console.log(`  ${chalk.cyan(`npm install`)}`);
  console.log(`  ${chalk.cyan(`npm run start-keycloak &`)}`);
  console.log();
  console.log(`  ${chalk.cyan(`npm run dev`)}`);
  console.log();
  console.log("👾 🚀 Happy hacking!");
}

main();
