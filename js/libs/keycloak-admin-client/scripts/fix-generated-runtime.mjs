/**
 * Post-processing script to fix OpenAPI Generator TypeScript output for Node.js compatibility.
 * 
 * Fixes:
 * 1. Adds missing type definitions for Node.js (RequestCredentials)
 * 2. Fixes type compatibility issues in runtime.ts
 * 
 * Note: Import file extensions are handled by the generator's importFileExtension option.
 */

import { readFileSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const generatedDir = join(__dirname, '..', 'src', 'generated');

/**
 * Fix runtime.ts for Node.js compatibility
 */
function fixRuntime(content) {
  // Add type definitions at the top of the file (after the eslint-disable comments)
  const typeDefinitions = `
// Node.js compatibility types
type RequestCredentials = 'include' | 'omit' | 'same-origin';
`;

  // Replace WindowOrWorkerGlobalScope['fetch'] with typeof fetch
  let fixed = content.replace(
    /export type FetchAPI = WindowOrWorkerGlobalScope\['fetch'\];/g,
    'export type FetchAPI = typeof fetch;'
  );

  // Add type definitions after the eslint-disable comment block
  if (!fixed.includes('// Node.js compatibility types')) {
    fixed = fixed.replace(
      /(\* Do not edit the class manually\.\s*\*\/)\s*\n/,
      '$1\n' + typeDefinitions + '\n'
    );
  }

  // Fix 'response' possibly undefined errors
  fixed = fixed.replace(
    /throw new ResponseError\(response, 'Response returned an error code'\);/g,
    "throw new ResponseError(response!, 'Response returned an error code');"
  );

  fixed = fixed.replace(
    /response: response\.clone\(\),/g,
    'response: response!.clone(),'
  );

  // Fix fetchApi type compatibility in middleware contexts
  fixed = fixed.replace(
    /fetch: this\.fetchApi,/g,
    'fetch: this.fetchApi as FetchAPI,'
  );

  return fixed;
}

function main() {
  console.log('Fixing OpenAPI Generator output for Node.js compatibility...\n');
  
  const runtimePath = join(generatedDir, 'runtime.ts');
  
  try {
    const content = readFileSync(runtimePath, 'utf-8');
    const fixed = fixRuntime(content);
    
    if (content !== fixed) {
      writeFileSync(runtimePath, fixed);
      console.log(`  Fixed: ${runtimePath}`);
      console.log('\n✅ Applied runtime fixes');
    } else {
      console.log('✅ No fixes needed');
    }
  } catch (error) {
    if (error.code === 'ENOENT') {
      console.log('⚠️  runtime.ts not found, skipping');
    } else {
      throw error;
    }
  }
}

main();
