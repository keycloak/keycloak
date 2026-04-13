/**
 * Generates JS examples for Admin API v2 documentation.
 *
 * Walks Kiota-generated NavigationMetadata to discover operations,
 * matches them with operationIds from openapi.json, and produces:
 *   - src/generated/doc-examples/admin-v2-js-examples.json
 *   - src/generated/doc-examples/admin-v2-doc-examples-check.ts
 *   - src/generated/doc-examples/tsconfig.doc-check.json
 */

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import type { NavigationMetadata } from "@microsoft/kiota-abstractions";
import { AdminClientNavigationMetadata } from "../src/generated/adminClient.js";

const projectDir = join(dirname(fileURLToPath(import.meta.url)), "..");
const outputDir = join(projectDir, "src", "generated", "doc-examples");

const BASEURL_PREFIX = "{+baseurl}";
const VERIFY_ARG = "undefined as any";

// Maps the Kiota navigation chain to the user-facing wrapper API.
// When new v2 resources are added, add entries here.
const WRAPPERS = [
  {
    prefix: "kcAdminClient.clients.v2()",
    chainPrefix: ".admin.api.byRealmName(realmName).clients.v2",
  },
];

const spec = JSON.parse(
  readFileSync(join(projectDir, "openapi.json"), "utf-8"),
);
const operationIds = new Map<string, string>();
for (const [path, methods] of Object.entries(spec.paths || {})) {
  for (const [method, operation] of Object.entries(
    methods as Record<string, any>,
  )) {
    if (operation?.operationId) {
      operationIds.set(
        `${method.toUpperCase()}:${path}`,
        operation.operationId,
      );
    }
  }
}

const examples: Record<string, { example: string }> = {};
const navParamNames = new Set<string>();

function collectEndpointExamples(
  navEntries: Record<string, NavigationMetadata>,
  chain: string,
): void {
  for (const [key, meta] of Object.entries(navEntries)) {
    const params = meta.pathParametersMappings;
    const segment = params?.length
      ? `.${key}(${params.join(", ")})`
      : `.${key}`;
    const currentChain = chain + segment;

    if (params) {
      params.forEach((p) => navParamNames.add(p));
    }

    if (meta.requestsMetadata) {
      for (const [method, reqMeta] of Object.entries(meta.requestsMetadata)) {
        const rawTemplate = reqMeta.uriTemplate as string;
        if (!rawTemplate.startsWith(BASEURL_PREFIX)) {
          throw new Error(
            `Cannot match operationId — URI template missing ${BASEURL_PREFIX} prefix: ${rawTemplate}`,
          );
        }
        const uriTemplate = rawTemplate.slice(BASEURL_PREFIX.length);
        const operationId = operationIds.get(
          `${method.toUpperCase()}:${uriTemplate}`,
        );
        if (!operationId) {
          throw new Error(
            `Cannot generate example — no operationId in openapi.json for ${method.toUpperCase()} ${uriTemplate}`,
          );
        }

        const wrapper = WRAPPERS.find((w) =>
          currentChain.startsWith(w.chainPrefix),
        );
        if (!wrapper) {
          throw new Error(
            `Cannot generate example — no wrapper prefix configured in generate-doc-examples.ts for: ${currentChain}`,
          );
        }

        const remainder = currentChain.slice(wrapper.chainPrefix.length);
        const contentType = reqMeta.requestBodyContentType as
          | string
          | undefined;
        // Kiota metadata does not expose method parameter count;
        // if a method ever has more than one argument, this needs updating
        const bodyArg = contentType ? "requestBody" : "";

        const call = `${wrapper.prefix}${remainder}.${method}(${bodyArg});`;
        examples[operationId] = { example: call };
      }
    }

    if (meta.navigationMetadata) {
      collectEndpointExamples(
        meta.navigationMetadata as Record<string, NavigationMetadata>,
        currentChain,
      );
    }
  }
}

collectEndpointExamples(
  AdminClientNavigationMetadata as unknown as Record<
    string,
    NavigationMetadata
  >,
  "",
);

if (Object.keys(examples).length === 0) {
  throw new Error(
    "Documentation build failed — no JS examples were generated from Kiota metadata",
  );
}

mkdirSync(outputDir, { recursive: true });

writeFileSync(
  join(outputDir, "admin-v2-js-examples.json"),
  JSON.stringify(examples, null, 2) + "\n",
);

navParamNames.add("requestBody");
const paramDeclarations = [...navParamNames]
  .map((p) => `const ${p} = ${VERIFY_ARG};`)
  .join("\n");

const verifyContent =
  `import type { KeycloakAdminClient } from "../../client.js";\n` +
  `declare const kcAdminClient: KeycloakAdminClient;\n` +
  paramDeclarations +
  "\n" +
  Object.values(examples)
    .map((e) => e.example)
    .join("\n") +
  "\n";

writeFileSync(join(outputDir, "admin-v2-doc-examples-check.ts"), verifyContent);

writeFileSync(
  join(outputDir, "tsconfig.doc-check.json"),
  JSON.stringify(
    {
      extends: "../../../tsconfig.json",
      include: ["admin-v2-doc-examples-check.ts"],
      compilerOptions: { noEmit: true },
    },
    null,
    2,
  ) + "\n",
);
