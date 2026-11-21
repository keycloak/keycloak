#!/usr/bin/env node

import chalk from "chalk";
import { Command, InvalidArgumentError } from "commander";
import fs from "fs-extra";
import Mustache from "mustache";
import { mkdtemp } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { createInterface } from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { simpleGit } from "simple-git";
import { fileURLToPath } from "url";

const packageJson = JSON.parse(
  fs.readFileSync(
    join(fileURLToPath(import.meta.url), "..", "package.json"),
    "utf8",
  ),
);

const TYPES = ["account", "admin"];

function parseType(value) {
  if (!TYPES.includes(value)) {
    throw new InvalidArgumentError("It should be either account or admin");
  }
  return value;
}

async function promptType() {
  const rl = createInterface({ input, output });
  try {
    for (;;) {
      const answer = (
        await rl.question(
          `Select a type (${TYPES.map((t) => chalk.green(t)).join(" / ")}): `,
        )
      )
        .trim()
        .toLowerCase();

      if (TYPES.includes(answer)) {
        return answer;
      }

      console.log(
        chalk.red(`Invalid type. Choose one of: ${TYPES.join(", ")}`),
      );
    }
  } finally {
    rl.close();
  }
}

function main() {
  new Command(packageJson.name)
    .version(packageJson.version)
    .description(packageJson.description)
    .argument("<name>", "name of the project")
    // Optional positional type: `npm create` treats `-t` as its own `--tag`
    // flag, so `npm create keycloak-theme my-theme -t admin` arrives here as
    // `my-theme admin`.
    .argument("[type]", "account or admin", parseType)
    .usage(`${chalk.green("<name>")} ${chalk.green("[type]")} [options]`)
    .option(
      "-t, --type <type>",
      "the type of ui to be created either `account` or `admin`",
      parseType,
    )
    .action(async (name, typeArg, options) => {
      const type = options.type || typeArg || (await promptType());
      console.log(
        `Creating a new ${chalk.green(name)} project of type ${chalk.green(type)}`,
      );

      await createProject(name, type);
      done(name);
    })
    .on("--help", () => {
      console.log();
      console.log(`    Only ${chalk.green("<name>")} is required.`);
      console.log(
        `    If ${chalk.green("[type]")} / ${chalk.blue("--type")} is omitted, you will be prompted.`,
      );
      console.log();
      console.log(
        `    Type can be passed as ${chalk.green("[type]")} or ${chalk.blue("--type")}:`,
      );
      console.log(
        `      - ${chalk.green("account")} for an account ui based ui`,
      );
      console.log(`      - ${chalk.green("admin")} for an admin ui based ui`);
      console.log();
      console.log("    Examples:");
      console.log(
        `      ${chalk.cyan("npm create keycloak-theme my-theme admin")}`,
      );
      console.log(
        `      ${chalk.cyan("npm create keycloak-theme my-theme -- --type admin")}`,
      );
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
        .then(() => resolve(join(dir, `extension/extend-${type}-console-node`)))
        .catch(reject);
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
