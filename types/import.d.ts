// ESM-HMR Interface: `import.meta.hot`

interface ImportMeta {
  // TODO: Import the exact .d.ts files from "esm-hmr"
  // https://github.com/pikapkg/esm-hmr
  hot: any;
  env: Record<string, any>;
}

declare namespace Cypress {
  interface Chainable<Subject> {
    /**
     * Get one or more DOM elements by `data-testid`.
     *
     * @example
     * cy.getId('searchButton')  // Gets the <button data-testid="searchButton">Search</button>
     */
    getId(selector: string, ...args): Chainable<any>;
  }
}
