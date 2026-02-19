// Use require for CommonJS compatibility with @microsoft/kiota
// The ESM build of @microsoft/kiota is broken (missing files), so we use require()
import { createRequire } from "module";
import { fileURLToPath } from "url";
import { dirname, resolve } from "path";
import { existsSync, mkdirSync, writeFileSync, readFileSync } from "fs";

const require = createRequire(import.meta.url);
const {
  generateClient,
  KiotaGenerationLanguage,
  ConsumerOperation,
} = require("@microsoft/kiota");

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const projectRoot = resolve(__dirname, ".");

// Configuration
const OPENAPI_URL = process.env.OPENAPI_URL || "http://localhost:9000/openapi";
const OPENAPI_FILE = process.env.OPENAPI_FILE; // Optional: use a local file instead
const OUTPUT_PATH = resolve(projectRoot, "src/generated");
const CLIENT_CLASS_NAME = "AdminClient";
const CLIENT_NAMESPACE = "ApiSdk";

async function downloadOpenApiSpec(url: string): Promise<string> {
  console.log(`📥 Downloading OpenAPI spec from ${url}...`);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(
      `Failed to download OpenAPI spec: ${response.status} ${response.statusText}`,
    );
  }
  const content = await response.text();

  // Save to a temp file
  const tempFile = resolve(projectRoot, ".openapi-temp.yaml");
  writeFileSync(tempFile, content);
  console.log(`✅ Downloaded and saved to ${tempFile}`);
  return tempFile;
}

async function main() {
  console.log("🚀 Keycloak Admin Client v2 - Kiota Generator\n");

  let openApiFilePath: string;

  if (OPENAPI_FILE) {
    // Use local file
    openApiFilePath = resolve(projectRoot, OPENAPI_FILE);
    if (!existsSync(openApiFilePath)) {
      console.error(`❌ OpenAPI file not found: ${openApiFilePath}`);
      process.exit(1);
    }
    console.log(`📄 Using local OpenAPI file: ${openApiFilePath}`);
  } else {
    // Download from URL
    openApiFilePath = await downloadOpenApiSpec(OPENAPI_URL);
  }

  // Ensure output directory exists
  if (!existsSync(OUTPUT_PATH)) {
    mkdirSync(OUTPUT_PATH, { recursive: true });
  }

  console.log(`\n📦 Generating TypeScript client...`);
  console.log(`   Output: ${OUTPUT_PATH}`);
  console.log(`   Client class: ${CLIENT_CLASS_NAME}`);
  console.log(`   Namespace: ${CLIENT_NAMESPACE}\n`);

  try {
    const result = await generateClient({
      openAPIFilePath: openApiFilePath,
      clientClassName: CLIENT_CLASS_NAME,
      clientNamespaceName: CLIENT_NAMESPACE,
      language: KiotaGenerationLanguage.TypeScript,
      outputPath: OUTPUT_PATH,
      operation: ConsumerOperation.Generate,
      workingDirectory: projectRoot,
      cleanOutput: true,
    });

    if (result) {
      console.log("\n✅ Client generated successfully!");

      if (result.logs && result.logs.length > 0) {
        console.log("\n📋 Generation logs:");
        for (const log of result.logs) {
          const level = log.level === 1 ? "⚠️" : log.level === 2 ? "❌" : "ℹ️";
          console.log(`   ${level} ${log.message}`);
        }
      }
    } else {
      console.log("\n⚠️ Generation completed but returned no result");
    }
  } catch (error) {
    console.error("\n❌ Generation failed:", error);
    process.exit(1);
  }

  // Post-process to fix Kiota npm package bug with empty serializer registration blocks
  postProcessGeneratedCode();

  console.log("\n🎉 Done! You can now build the project with: npm run build");
}

/**
 * Fix Kiota npm package bug: it generates empty if-blocks for serializer registration.
 * This is a known issue - the serializers/deserializers options don't work in the npm API.
 */
function postProcessGeneratedCode() {
  const adminClientPath = resolve(OUTPUT_PATH, "adminClient.ts");

  if (!existsSync(adminClientPath)) {
    console.log("⚠️ adminClient.ts not found, skipping post-processing");
    return;
  }

  console.log("\n🔧 Post-processing generated code...");

  let content = readFileSync(adminClientPath, "utf-8");
  let modified = false;

  // Check if the if-blocks are empty (missing the registration calls)
  // Handle both Unix (\n) and Windows (\r\n) line endings
  if (
    content.includes(
      "if (parseNodeFactoryRegistry.registerDefaultDeserializer) {\n    }",
    ) ||
    content.includes(
      "if (parseNodeFactoryRegistry.registerDefaultDeserializer) {\r\n    }",
    )
  ) {
    // Add required imports for serializers/deserializers
    const serializerImports = `import { FormParseNodeFactory, FormSerializationWriterFactory } from "@microsoft/kiota-serialization-form";
import { JsonParseNodeFactory, JsonSerializationWriterFactory } from "@microsoft/kiota-serialization-json";
import { MultipartSerializationWriterFactory } from "@microsoft/kiota-serialization-multipart";
import { TextParseNodeFactory, TextSerializationWriterFactory } from "@microsoft/kiota-serialization-text";
`;

    // Add imports after the existing imports
    content = content.replace(
      /import \{ apiClientProxifier,/,
      serializerImports + "import { apiClientProxifier,",
    );

    // Fill in the deserializer registration (without if-check to avoid TypeScript 5.9+ warnings)
    // Handle both Unix (\n) and Windows (\r\n) line endings
    content = content.replace(
      /if \(parseNodeFactoryRegistry\.registerDefaultDeserializer\) \{(?:\r?\n) {4}\}/,
      `parseNodeFactoryRegistry.registerDefaultDeserializer(JsonParseNodeFactory, backingStoreFactory);
    parseNodeFactoryRegistry.registerDefaultDeserializer(TextParseNodeFactory, backingStoreFactory);
    parseNodeFactoryRegistry.registerDefaultDeserializer(FormParseNodeFactory, backingStoreFactory);`,
    );

    // Fill in the serializer registration (without if-check to avoid TypeScript 5.9+ warnings)
    // Handle both Unix (\n) and Windows (\r\n) line endings
    content = content.replace(
      /if \(serializationWriterFactory\.registerDefaultSerializer\) \{(?:\r?\n) {4}\}/,
      `serializationWriterFactory.registerDefaultSerializer(JsonSerializationWriterFactory);
    serializationWriterFactory.registerDefaultSerializer(TextSerializationWriterFactory);
    serializationWriterFactory.registerDefaultSerializer(FormSerializationWriterFactory);
    serializationWriterFactory.registerDefaultSerializer(MultipartSerializationWriterFactory);`,
    );

    modified = true;
    console.log("   ✅ Added serializer/deserializer registration code");
  }

  if (modified) {
    writeFileSync(adminClientPath, content);
  } else {
    console.log("   ℹ️ No fixes needed");
  }
}

void main();
