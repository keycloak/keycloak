# Coding Guidelines

## Package management

The default package manager is PNPM, we recommend enabling [Corepack](https://nodejs.org/api/corepack.html) for the best compatibility, which will automatically ensure the correct version of PNPM is used:

```sh
corepack enable
```

There are several reasons why PNPM is used over other package managers (such as NPM and Yarn):

- The reasons mentioned in [pnpm vs npm](https://pnpm.io/pnpm-vs-npm), mostly it avoids [silly bugs](https://www.kochan.io/nodejs/pnpms-strictness-helps-to-avoid-silly-bugs.html).
- Unlike [`npm ci`](https://docs.npmjs.com/cli/v9/commands/npm-ci) it preserves the `node_modules` directory between installs, allowing for faster install times (especially in Maven builds).
- Unlike NPM it does not require workspace dependencies to be [explicitly versioned](https://pnpm.io/workspaces#publishing-workspace-packages), simplifying release versioning.

If you submit a pull request that changes the dependencies, make sure that you also update the `pnpm-lock.yaml` as well.

Since this project relies greatly on [PNPM workspaces](https://pnpm.io/workspaces) it is recommended you familiarize yourself with features such as [`--filter`](https://pnpm.io/filtering).

## Running

We run the UI in dev mode and configure keycloak to proxy the request this can be done by adding `KC_ADMIN_VITE_URL=http://localhost:5174` as environment variable to the startup of keycloak or by using the start script:

```bash
pnpm --filter keycloak-server start --admin-dev
```

This script will download the nightly version of keycloak and start it with the added environment variable.
Afterwards one needs to start the dev server with:

```bash
pnpm --filter keycloak-admin-ui run dev
```

Now when viewing the admin ui in the browser this is the local version and any changes made to the ui code base will automatically be reflected no reload needed.
See [README.md](apps/keycloak-server/README.md) for more information.

## Code-style

### Frameworks used

We try and keep the use of frameworks to a minium, but we do use a couple:
  1. [Patternfly](https://www.patternfly.org/) a component library
  1. [React router](https://reactrouter.com/) for routing
  1. [React hook form](https://react-hook-form.com/) with so many forms this is to keep state
  1. [Playwright](https://playwright.dev/) for testing


### Linting

To ensure code-style is consistent between various contributions [ESLint](https://eslint.org/) is used to enforce a common set of guidelines. The [recommended rules](https://eslint.org/docs/latest/rules/) of ESLint are used as a foundation.

For TypeScript code-style the recommendations of [`typescript-eslint`](https://typescript-eslint.io/) are adhered to as much as possible, specifically the [`strict-type-checked`](https://typescript-eslint.io/users/configs#strict-type-checked) and [`stylistic-type-checked`](https://typescript-eslint.io/users/configs#stylistic-type-checked) configurations.

Deviations from, or additions to these rules should be documented by comments in the [ESLint configuration](.eslintrc.cjs).

### Non-null assertion operator

The [non-null assertion operator](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-0.html#non-null-assertion-operator) (`!`) is sometimes used to tell the TypeScript compiler that it is guaranteed that a value is not `null` or `undefined`. Because this might possibly introduce errors at run-time it should be used sparingly.

The only place where it is valid to use the non-null assertion operator is on the types that are provided by the [Admin API client](https://github.com/keycloak/keycloak-nodejs-admin-client). The reason for this is that the types are generated from Java code, which does not explicitly provide information about the nullability of fields (more on that [here](https://github.com/keycloak/keycloak-nodejs-admin-client/issues/187)).

## State management

We have made a conscious decision to stay away from state management technologies such as Redux. These overarching state management schemes tend to be overly complex and encourage dumping everything into the global state.

Instead, we are following a simple philosophy that state should remain close to where it is used and moved to a wider scope only as truly needed. This encourages encapsulation and makes management of the state much simpler.

The way this plays out in our application is that we first prefer state to remain in the scope of the component that uses it. If the state is required by more than one component, we move to a more complex strategy for management of that state. In other words, in order of preference, state should be managed by:

1. Storing in the component that uses it.
2. If #1 is not sufficient, [lift state up](https://reactjs.org/docs/lifting-state-up.html).
3. If #2 is not sufficient, try [component composition](https://reactjs.org/docs/context.html#before-you-use-context).
4. If #3, is not sufficient, use a [global context](https://reactjs.org/docs/context.html).

A good tutorial on this approach is found in [Kent Dodds’ blog](https://kentcdodds.com/blog/application-state-management-with-react).

## Hooks

When using hooks with Typescript there are few recommendations that we follow below. Additional recommendations besides the ones mentioned in this document can be found [here](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks).

### Inference vs Types for useState

Currently we recommend using inference for the primitive types booleans, numbers, and strings when using useState. Anything other then these 3 types should use a declarative syntax to specify what is expected. For example the following is an example of how to use inference:

```javascript
const [isEnabled, setIsEnabled] = useState(false);
```

Here is an example how to use a declarative syntax. When using a declarative syntax, if the value can be null, that will also need to be specified:

```javascript
const [user, setUser] = useState<IUser | null>(null);

...

setUser(newUser);


```

### useEffect

For useEffect only [return the function or undefined](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks#useeffect).

## CSS

We use custom CSS in rare cases where PatternFly styling does not meet our design guidelines. If styling needs to be added, we should first check that the PatternFly component is being properly built and whether a variant is already provided to meet the use case. Next, PatternFly layouts should be used for most positioning of components. For one-off tweaks (e.g. spacing an icon slightly away from the text next to it), a PatternFly utility class should be used. In all cases, PatternFly variables should be used for colors, spacing, etc. rather than hard coding color or pixel values.

We will use one global CSS file to surface customization variables. Styles particular to a component should be located in a .CSS file within the component’s folder. A modified BEM naming convention should be used as detailed below.

### Location of files, location of classes

- Global styling should be located…? _./public/index.css_.

- The CSS relating to a single component should be located in a file within each component’s folder.

### Naming CSS classes

PatternFly reference https://pf4.patternfly.org/guidelines#variables

For the Admin UI, we modify the PatternFly convention to namespace the classes and variables to the Keycloak packages.

**Class name**

```css
.keycloak-admin--block[__element][--modifier][--state][--breakpoint][--pseudo-element]
```

**Examples of custom CSS classes**

```css
// Modification to all data tables throughout Keycloak admin
.keycloak-admin--data-table {
...
}

// Data tables throughout keycloak that are marked as compact
.keycloak-admin--data-table--compact {
...
}

// Child elements of a compact data-table
// Don’t increase specificity with a selector like this:
// .keycloak-admin--data-table--compact .data-table-item
// Instead, follow the pattern for a single class on the child
.keycloak-admin--data-table__data-table-item--compact {
...
}

// Compact data table just in the management UI at the lg or higher breakpoint
.keycloak-admin--data-table--compact--lg {
...
}
```

### Naming CSS custom properties and using PatternFly’s custom properties

Usually, PatternFly components will properly style components. Sometimes problems with the spacing or other styling indicate that a wrapper component is missing or that components haven’t been put together quite as intended. Often there is a variant of the component available that will accomplish the design.

However, there are other times when modifications must be made to the styling provided by PatternFly, or when styling a custom component. In these cases, PatternFly custom properties (CSS variables) should be used as attribute values. PatternFly defines custom properties for colors, spacing, border width, box shadow, and more. Besides a full color palette, colors are defined specifically for borders, statuses (success, warning, danger, info), backgrounds, etc.

These values can be seen in the [PatternFly design guidelines](https://v4-archive.patternfly.org/v4/design-guidelines/styles/colors) and a [full listing of variables](https://v4-archive.patternfly.org/v4/developer-resources/global-css-variables) can be found in the documentation section.

For the Admin UI, we modify the PatternFly convention to namespace the classes and variables to the Keycloak packages.

**Custom property**

```css
--keycloak-admin--block[__element][--modifier][--state][--breakpoint][--pseudo-element]--PropertyCamelCase
```

**Example of a CSS custom property**

```css
// Modify the height of the brand image
--keycloak-admin--brand--Height: var(--pf-v5-global--spacer--xl);
```

**Example**

```css
// Don’t increase specificity
// Don’t use pixel values
.keycloak-admin--manage-columns__modal .pf-v5-c-dropdown {
  margin-bottom: 24px;
}

// Do use a new class
// Do use a PatternFly global spacer variable
.keycloak-admin--manage-columns__dropdown {
  margin-bottom: var(--pf-v5-global--spacer--xl);
}
```

### Using utility classes

Utility classes can be used to add specific styling to a component, such as margin-bottom or padding. However, their use should be limited to one-off styling needs.

For example, instead of using the utility class for margin-right multiple times, we should define a new Admin UI class that adds this _margin-right: var(--pf-v5-global--spacer--sm);_ and in this example, the new class can set the color appropriately as well.

**Using a utility class **

```js
switch (titleStatus) {
  case "success":
    return (
      <>
        <InfoCircleIcon
          className="pf-v5-u-mr-sm" // utility class
          color="var(--pf-v5-global--info-color--100)"
        />{" "}
        {titleText}
      </>
    );
  case "failure":
    return (
      <>
        <InfoCircleIcon
          className="pf-v5-u-mr-sm"
          color="var(--pf-v5-global--danger-color--100)"
        />{" "}
        {titleText}
      </>
    );
}
```

**Better way with a custom class**

```js
switch (titleStatus) {
  case "success":
    return (
      <>
        <InfoCircleIcon
          className="keycloak-admin--icon--info" // use a new keycloak class
        />{" "}
        {titleText}{" "}
      </>
    );
  case "failure":
    return (
      <>
        <InfoCircleIcon className="keycloak-admin--icon--info" /> {titleText}{" "}
      </>
    );
}
```

## Resources

- [PatternFly Docs](https://www.patternfly.org/)
- [React hook form](https://react-hook-form.com/)
- [Learn React](https://react.dev/learn)
- [Typescript](https://www.typescriptlang.org/docs/handbook/intro.html)
- [Playwright](https://playwright.dev/)
