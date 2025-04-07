import fs from "node:fs/promises";
import path from "node:path";

const targetDir = "target/classes/theme/keycloak/common/resources/vendor";

replaceContents(
  path.join(targetDir, "react/react-jsx-runtime.production.min.js"),
  '"./react.production.min.js"',
  '"react"',
);

async function replaceContents(filePath, search, replace) {
  const file = await fs.readFile(filePath, "utf8");
  const newFile = file.replace(search, replace);

  await fs.writeFile(filePath, newFile);
}
