# Keycloak Account UI (Lit/Web Components)

A new version of the Keycloak Account Console built with [Lit](https://lit.dev/) and PatternFly 6 CSS.

## Features

- **Modern Web Components**: Built entirely with Lit and custom elements
- **PatternFly 6**: Uses PatternFly 6 CSS classes for consistent styling
- **No React dependency**: Framework-agnostic implementation
- **No build step required**: Pure JavaScript ES modules - no TypeScript, no bundler
- **Dynamic page loading**: Pages loaded on-demand based on content.json
- **Same functionality**: All features from the React version are available

## Zero Build Architecture

This theme requires **no Node.js or build tools** to use. The source files are plain JavaScript ES modules that run directly in the browser via import maps.

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser                                  │
├─────────────────────────────────────────────────────────────┤
│  Import Map (resolves bare specifiers to vendor files)       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ lit → /vendor/lit/lit.js                                 ││
│  │ keycloak-js → /vendor/keycloak-js/keycloak.js           ││
│  │ i18next → /vendor/i18next/i18next.js                    ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  main.js → components/kc-app.js                             │
│       ↓                                                      │
│  content.json (defines routes & page components)            │
│       ↓                                                      │
│  Dynamic import: pages/personal-info.js, pages/groups.js... │
└─────────────────────────────────────────────────────────────┘
```

## Pages

- **Personal Info**: View and edit user profile information
- **Account Security**:
  - Signing In: Manage authentication credentials
  - Device Activity: View and manage active sessions
  - Linked Accounts: Link/unlink external identity providers
- **Applications**: Manage application consents
- **Groups**: View group memberships
- **Organizations**: View organization memberships
- **Resources**: Manage UMA resources (when enabled)

## Development

### Prerequisites (for development only)

- Node.js 18+ (only needed for Vite dev server)

### Development Server

```bash
npm install
npm run dev
```

This starts the Vite development server at `http://localhost:5174`.

### Build (for distribution)

```bash
npm run build
```

This simply copies the JavaScript source files to `target/classes/theme/keycloak.v3/account/resources`. No compilation or bundling is performed.

## Project Structure

```
src/
├── api/                 # API methods and type definitions
│   ├── fetch-content.js # Loads content.json
│   ├── methods.js       # API request functions
│   ├── representations.js # JSDoc type definitions
│   └── request.js       # HTTP request utility
├── components/          # Reusable web components
│   ├── kc-app.js        # Main app shell (masthead, sidebar, routing)
│   ├── kc-nav.js        # Navigation sidebar (driven by content.json)
│   └── ui/              # Reusable UI component helpers
│       ├── kc-button.js
│       ├── kc-input.js
│       ├── kc-select.js
│       ├── kc-dropdown.js
│       ├── kc-spinner.js
│       ├── kc-alert.js
│       ├── kc-empty-state.js
│       └── index.js     # Re-exports all UI components
├── pages/               # Page components (loaded dynamically)
│   ├── personal-info.js
│   ├── signing-in.js
│   ├── device-activity.js
│   ├── linked-accounts.js
│   ├── applications.js
│   ├── groups.js
│   ├── organizations.js
│   └── resources.js
├── types/               # Shared type definitions (JSDoc)
│   └── menu.js          # MenuItem type
├── utils/               # Utility functions
├── environment.js       # Environment configuration
├── i18n.js              # Internationalization
├── keycloak-context.js  # Lit context for Keycloak instance
└── main.js              # Application entry point
public/
└── content.json         # Navigation and routing configuration
```

## Adding a New Page

1. Create the page component in `src/pages/my-page.js`:

```javascript
import { LitElement, html } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { t } from "../i18n.js";

export class KcMyPage extends LitElement {
  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("myPageTitle")}</h1>
        <p>${t("myPageDescription")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          <p>Page content here</p>
        </div>
      </div>
    `;
  }
}

customElements.define("kc-my-page", KcMyPage);
```

2. Add entry to `public/content.json`:

```json
{
  "label": "myPage",
  "path": "my-page",
  "component": "kc-my-page",
  "modulePath": "./pages/my-page.js"
}
```

3. Add translation keys to the theme's `messages_en.properties`.

No changes needed to `kc-app.js` or `kc-nav.js`.

## Key Technologies

- **[Lit](https://lit.dev/)**: Fast, lightweight web component library
- **[@patternfly/patternfly](https://www.patternfly.org/)**: PatternFly 6 CSS
- **[@lit/context](https://lit.dev/docs/data/context/)**: Dependency injection for Lit
- **[keycloak-js](https://www.keycloak.org/docs/latest/securing_apps/#_javascript_adapter)**: Keycloak JavaScript adapter
- **[i18next](https://www.i18next.com/)**: Internationalization framework
- **Import Maps**: Browser-native module resolution

## Benefits

- **No build step**: Source files are served directly (just copied during Maven build)
- **No Node.js required**: Theme users don't need to install Node.js
- **Better caching**: Change one file, only that file is invalidated
- **Smaller initial load**: Pages loaded on-demand
- **Easier debugging**: Source matches what runs in browser
- **Framework agnostic**: Web Components work anywhere

## License

Apache License 2.0
