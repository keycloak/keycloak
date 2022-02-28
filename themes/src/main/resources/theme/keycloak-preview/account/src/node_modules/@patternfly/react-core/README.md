# @patternfly/react-core

This package provides Core PatternFly components for the [PatternFly 4][patternfly-4].

### Prerequisite

#### Node Environment

This project currently supports Node [Active LTS](https://github.com/nodejs/Release#release-schedule) releases. Please stay current with Node Active LTS when developing patternfly-react.

For example, to develop with Node 8, use the following:

```
nvm install 10
nvm use 10
```

This project also requires a Yarn version of >=1.6.0. The latest version can be installed [here](https://yarnpkg.com/).

### Installing

```
yarn add @patternfly/react-core
```

or

```
npm install @patternfly/react-core --save
```

# Usage

#### Pre-requisites

It's strongly advised to use the PatternFly Base CSS in your whole project, or some components may diverge in appearance:

```javascript
import '@patternfly/react-core/dist/styles/base.css';
```

#### Example Component Usage

```javascript
import { Button } from '@patternfly/react-core';

export default <Button variant="primary">Button</Button>;
```

All css related to each component is provided within it. There is no component level CSS to import.

# Applying Red Hat Fonts (Optional)
If you would like to use Red Hat fonts instead of Overpass, simply add the class `.pf-m-redhat-font` to an element that wraps your application (ideally `<html>` or `<body>`) to adopt the CSS changes that introduce the Red Hat fonts into PatternFly.


# Documentation

This project uses Gatsby. For an overview of the project structure please refer to the [Gatsby documentation - Building with Components](https://www.gatsbyjs.org/docs/building-with-components/).

A comprehensive list of components and detailed usage of each can be found on the [PatternFly React Docs][docs] website
You can also find how each component is meant to be used from a design perspective on the [PatternFly 4 Core][patternfly-4] website.

Note: All commands below assume you are on the root directory in this repository.

### Install

Run to install all the dependencies.

```sh
yarn install && yarn build
```

### Running

To launch the development server and view the workspace:

```sh
yarn start:pf4
```

Go to localhost:8000.

### Building

To build the site:

```sh
yarn build:docs
```

# Contributing Components

This library makes use of the babel plugin from [@patternfly/react-styles](../react-styles/README.md) to enable providing the CSS alongside the components. This removes the need for consumers to use (style|css|sass)-loaders. For an example of using CSS from core you can reference [Button.js](./src/components/Button/Button.js). For any CSS not provided by core please use the `StyleSheet.create` utility from [@patternfly/react-styles](../react-styles/README.md). This will prevent collisions with any consumers, and allow the CSS to be bundled with the component.

### Building

```
yarn build
```

Note the build scripts for this are located in the root package.json under `yarn build`.

### Testing

Testing is done at the root of this repo. To only run the patternfly-react tests:

```
yarn test packages/patternfly-4/react-core
```

[patternfly-4]: https://github.com/patternfly/patternfly-next
[docs]: https://patternfly-react.surge.sh/patternfly-4


## Tree Shaking

Ensure optimization.sideEffects is set to true within your Webpack config:
```JS
optimization: {
  sideEffects: true
}
```

Use ESM module imports to enable tree shaking with no additional setup required.
```JS
import { TimesIcon } from '@patternfly/react-icons';
```

To enable tree shaking with named imports for CJS modules, utilize [babel-plugin-transform-imports](https://www.npmjs.com/package/babel-plugin-transform-imports) and update a babel.config.js file to utilize the plugin: 
```JS
require.extensions['.css'] = () => undefined;
const components = require('@patternfly/react-core/dist/js/components');
const beta = require('@patternfly/react-core/dist/js/beta');
const layouts = require('@patternfly/react-core/dist/js/layouts');

module.exports = {
  presets: ["@babel/preset-react"],
  plugins: [
    [
      "transform-imports",
      {
        "@patternfly/react-core": {
          transform: (importName, matches) => {
            let res = '@patternfly/react-core/dist/js/';
            if (components[importName]) {
              res += 'components';
            } else if (beta[importName]) {
              res += 'beta';
            } else if (layouts[importName]) {
              res += 'layouts';
            }

            res += `/${importName}/${importName}.js`;
            return res;
          },
          preventFullImport: true,
          skipDefaultConversion: true
        }
      }
    ]
  ]
}
```
