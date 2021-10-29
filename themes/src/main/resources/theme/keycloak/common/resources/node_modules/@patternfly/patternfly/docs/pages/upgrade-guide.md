---
id: upgrade-guide
title: Upgrading to PatternFly 4
---

Use the following steps to upgrade from PatternFly 3 to PatternFly 4.

**Before you begin**

- **Does you application require IE11 support?**
   - If the answer is yes, you can't upgrade to PatternFly 4. We're sorry!
- **Do you know what your build process is?**
  - Steps and requirements differ for basic HTML, Gulp, and Webpack, so be prepared with that information going in.
- **Do you need to run PatternFly 3 and PatternFly 4 together?**
  - If the answer is yes, use the following steps to update your configuration in the `src/patternfly/sass-utilities/scss-variables.scss` file to state `false`:

```scss
$pf-global--enable-reset: false !default;
```


## Installing PatternFly 4

Run the following commands to install:

```bash
npm install @patternfly/patternfly
```

----

## Configure your project

1. Navigate to the @patternfly/patternfly package you just installed and find the PatternFly 4 CSS stylesheet, `patternfly.css`. This file contains all of the updated PatternFly 4 styles.
2. Copy `patternfly.css` to your project's CSS directory.
3. In your HTML file, add the following line to the bottom of your list of CSS files to link to your new stylesheet:

```html noLive
<link rel="stylesheet" href="css/patternfly.css">
```

This will make it so that PatternFly 4 styles take precedence over anything that currently resides in your application.

## Building PatternFly 4

PatternFly 4 is distributed as separated modules:

- **Layouts** allow you to structure and organize the content on your pages
- **Components**, like buttons and alerts, can be assembled together to build applications

Each module delivers a sass file (`scss`) and CSS file so you can either include them in your build environment or consume the CSS from your page header.

- _If you need to overwrite any elements, we recommend extending the variables found in the `.scss` files, rather than manually overwriting the CSS._

All of PatternFly 4's components are kept under `@patternfly/patternfly/components/`.

All of PatternFly 4's layouts are kept under `@patternfly/patternfly/layouts/`.

### Build Examples

#### Webpack

_This example uses the following configuration:_

```json
webpack: "3.8.1",
sass-loader: "7.0.0",
css-loader: "^0.28.11",
style-loader: "^0.21.0",
sass: "^1.34.0"
```

_Code Snippets_

- Import all `.scss` files

```js noLive
module.exports = {
  module: {
    rules: [{
      test: /\.scss$/,
      use: [{
        loader: "style-loader"
      }, {
        loader: "css-loader"
      }, {
        loader: "sass-loader",
        options: {
          includePaths: [
            "../node_modules/@patternfly/patternfly/"
          ]
        }
      }]
    }]
  }
};
```

- Import select modules

```js noLive
module.exports = {
  module: {
    rules: [{
      test: /\.scss$/,
      use: [{
        loader: "style-loader"
      }, {
        loader: "css-loader"
      }, {
        loader: "sass-loader",
        options: {
          includePaths: [
            "../node_modules/@patternfly/patternfly/layouts/Page/",
            "../node_modules/@patternfly/patternfly/layouts/Grid",
            "../node_modules/@patternfly/patternfly/components/Content",
            "../node_modules/@patternfly/patternfly/components/SecondaryNav",
            "../node_modules/@patternfly/patternfly/components/Button"
          ]
        }
      }]
    }]
  }
};
```

----

## Typography

With PatternFly 4, we are switching from the Open Sans font family to Overpass. Additionally, we are updating the base font size from `12px` to `16px`, in order to increase readability and accessibility.

Overpass can be utilized in two different ways:

1. Built into PatternFly 4
    - By default, we include Overpass as part of the PatternFly 4 distributed CSS file. You do not need to do anything with your configuration to use this new font family.
1. Used as a CDN
    - If you wish to use the CDN for Overpass rather than the default approach, you will need to update the `sass-utilities/scss-variables.scss` file and build PatternFly 4 as part of your build process.
    - To use the CDN vs the standard build, update the `sass-utilities/scss-variables.scss` file as follows:

```scss
  $pf-global--enable-font-overpass-cdn: true !default;
```

## Icons

### PatternFly icons

TBD

### Font Awesome 5

Font Awesome 5 is now part of PatternFly 4, and we give you options for utilizing this icon library.

#### Font Awesome 5 packaged vs CDN

As part of PatternFly 4, we give users the option to either use Font Awesome 5 as an included set (prebuilt into the CSS), or as a CDN reference.

If you wish to use the CDN, you will need to build PatternFly 4 from source (`node_modules/@patternfly/patternfly/`) and update the `sass-utilities/scss-variables.scss` file as follows:

```scss
$pf-global--enable-fontawesome-cdn: true !default;
```

#### Font Awesome 4 & 5

If you are currently using Font Awesome 4 and just want to use Font Awesome 5 immediately, you will need to add the Font Awesome scripts:

```html noLive
<script defer src="https://use.fontawesome.com/releases/[VERSION]/js/all.js"></script>
<script defer src="https://use.fontawesome.com/releases/[VERSION]/js/v4-shims.js"></script>
```

This replaces the bundled Font Awesome 5 files in PatternFly 4, so your configuration file (`sass-utilities/scss-variables.scss`) will need to be updated to remove Font Awesome 5.

```scss
$pf-global--disable-fontawesome: true !default;
```

#### Font Awesome 5 tree shaking

Additionally, Font Awesome 5 now provides the option for [tree shaking](https://fontawesome.com/how-to-use/use-with-node-js#tree-shaking). In order to utilize this option, you will need to build PatternFly 4 from source (`node_modules/@patternfly/patternfly/`), and update the `sass-utilities/scss-variables.scss` file accordingly.

The updated `scss-variables.scss` file should look as follows:

```scss
$pf-global--disable-fontawesome: true !default;
```
