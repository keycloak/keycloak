[![Build Status](https://travis-ci.org/patternfly/patternfly.svg?branch=master)](https://travis-ci.org/patternfly/patternfly)
[![Dependency Status](https://gemnasium.com/badges/github.com/patternfly/patternfly.svg)](https://gemnasium.com/github.com/patternfly/patternfly)
[![Code Climate](https://codeclimate.com/github/patternfly/patternfly/badges/gpa.svg)](https://codeclimate.com/github/patternfly/patternfly)
[![NSP Status](https://nodesecurity.io/orgs/patternfly/projects/d8289b24-4ec5-4f7c-86c1-02aa2e6cf73d/badge)](https://nodesecurity.io/orgs/patternfly/projects/d8289b24-4ec5-4f7c-86c1-02aa2e6cf73d)
[![npm version](https://badge.fury.io/js/patternfly.svg)](https://badge.fury.io/js/patternfly)

[![Gitter](https://badges.gitter.im/patternfly/patternfly.svg)](https://gitter.im/patternfly/patternfly?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)


# [PatternFly](https://www.patternfly.org) reference implementation

This reference implementation of PatternFly is based on [Bootstrap v3](http://getbootstrap.com/).  Think of PatternFly as a "skinned" version of Bootstrap with additional components and customizations.  For information on how to quickly get started using PatternFly, see the [Quick Start Guide](QUICKSTART.md).  Looking for RCUE (Red Hat Common User Experience) information? See the [RCUE Quick Start Guide](RCUE-QUICKSTART.md).

If you wish to contribute to PatternFly, please follow the instructions under "Contributing to PatternFly".


# Installation

### Install with NPM

PatternFly can be installed and managed through [NPM](https://www.npmjs.com/). To do so, either add `patternfly` as a dependency in your `package.json` or run the following:

```sh
npm install patternfly --save
```

PatternFly stays up to date with the Node LTS [Release Schedule](https://github.com/nodejs/LTS#lts_schedule). If you're using PatternFly downstream, we suggest the use of an actively supported version of Node/NPM, although prior versions of Node may work.

### Install with Bower

PatternFly can be installed and managed through [Bower](http://bower.io/). To do so, either add `patternfly` as a dependency in your `bower.json` or run the following:

```sh
bower install patternfly --save
```

#### Using Wiredep?

Are you using [Wiredep](https://github.com/taptapship/wiredep)?  PatternFly's CSS includes the CSS of its dependencies.  As a result, you'll want to add the following to your [Wiredep configuration](https://github.com/taptapship/wiredep#configuration) so you don't end up with duplicate CSS.

```javascript
exclude: [
  "node_modules/patternfly/node_modules/patternfly-bootstrap-combobox/css/bootstrap-combobox.css",
  "node_modules/patternfly/node_modules/bootstrap-datepicker/dist/css/bootstrap-datepicker.css",
  "node_modules/patternfly/node_modules/bootstrap-datepicker/dist/css/bootstrap-datepicker3.css",
  "node_modules/patternfly/node_modules/bootstrap-select/dist/css/bootstrap-select.css",
  "node_modules/patternfly/node_modules/bootstrap-switch/dist/css/bootstrap3/bootstrap-switch.css",
  "node_modules/patternfly/node_modules/patternfly-bootstrap-treeview/dist/bootstrap-treeview.min.css",
  "node_modules/patternfly/node_modules/c3/c3.css",
  "node_modules/patternfly/node_modules/datatables/media/css/jquery.dataTables.css",
  "node_modules/patternfly/node_modules/datatables.net-colreorder-bs/css/colReorder.bootstrap.css",
  "node_modules/patternfly/node_modules/drmonty-datatables-colvis/css/dataTables.colVis.css",
  "node_modules/patternfly/node_modules/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.min.css",
  "node_modules/patternfly/node_modules/font-awesome/css/font-awesome.css",
  "node_modules/patternfly/node_modules/google-code-prettify/bin/prettify.min.css"
],
```

### Sass and/or Rails

**Patternfly now supports Sass natively!**
Sass is included in the `dist/sass` directory. Just add `node_modules` to your build tool's Sass include paths then `@import 'patternfly/dist/sass/patternfly';` in your Sass to get started!

#### Using Webpack?
There are two touch points for integrating patternfly sass: one in your webpack config, and another in your sass. Below is an example module rule for loading patternfly .scss files using webpack.

```javascript
module: {
  rules: [
    {
      test: /\.scss$/,
      use: [
        {
          loader: 'sass-loader',
          options: {
            includePaths: [
              // teach webpack to resolve these references
              path.resolve(__dirname, 'node_modules', 'patternfly', 'dist', 'sass'),
              path.resolve(__dirname, 'node_modules', 'bootstrap-sass', 'assets', 'stylesheets'),
              path.resolve(__dirname, 'node_modules', 'font-awesome-sass', 'assets', 'stylesheets')
            ]
          }
        }
      ]
    }
  ]
}
```

With webpack configured, just set the asset-path related variables and you're off!

```scss
$img-path: '~patternfly/dist/img/';
$font-path: '~patternfly/dist/fonts/';
$icon-font-path: '~patternfly/dist/fonts/';
@import '~patternfly/dist/sass/patternfly';
```

Please note that the [patternfly-sass](https://github.com/patternfly/patternfly-sass) is no longer supported and will not include any features or fixes introduced after Patternfly 3.23.2. However, the [patternfly-sass](https://rubygems.org/gems/patternfly-sass) Rubygem is maintained further and built from this repository.

### AngularJS

A set of [common AngularJS directives](https://github.com/patternfly/angular-patternfly) for use with PatternFly is available.

# Contributing to PatternFly

The following sections provide information on how to get started as a developer or designer in the PatternFly codebase.  In order to set up your environment, two different types of dependencies will need to be set up.  Please follow the instructions under "Development - Build Dependencies" (Node.js/Ruby) and "Development - Code Dependencies" below.  If you wish to use PatternFly in your project, please follow the [Quick Start Guide](QUICKSTART.md) instead.

## Development - Build Dependencies

Development setup requires Node.js and Ruby. If you do not already have Node.js, npm, and Ruby installed on your system, see https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager and https://www.ruby-lang.org/en/downloads.

## Development - Code Dependencies

The PatternFly code includes a number of dependencies that are not committed to this repository.  To add them, follow the instructions below under "Install NPM Dependencies".  Please make sure you keep them updated (see [Keeping NPM Dependencies Updated](#keeping-npm-dependencies-updated)).

## Development - Updating Dependencies

The npm-check-updates tool is available and configured to apply dependency updates to the project by running the command `npm run ncu`.  The package.json changes will have to be committed and a PR created.

## Autoprefixer

Patternfly uses [Autoprefixer](https://github.com/postcss/autoprefixer) to auto add prefixes to its output CSS. Since Patternfly extends some of the core Bootstrap3 less which contains prefixes, we also explicitly add prefixes in these cases to ensure backwards compatibility with Bootstrap3. If consuming Patternfly LESS and compiling, you can define your own target prefixes using [browserlist](https://github.com/ai/browserslist).

### Install NPM Dependencies

The development includes the use of a number of helpful tasks. In order to setup your development environment to allow running of these tasks, you need to install the local nodejs packages declared in `package.json`.

To do this clone, and change directories into PatternFly:

```sh
cd [PathToYourRepository]
```

then

```sh
npm install
```

This should take care of the majority of dependencies.

Since PatternFly is shrink wrapped, npm 3 will install all necessary development packages into `node_modules/patternfly/node_modules`. At this point, the gruntjs tasks are available for use such as starting a local development server or building the master CSS file.

If you prefer a flat dependency structure, you can define your own dependencies explicitly. That will flatten out the node_modules structure and place dependencies in the root node_modules directory.

Additionally you may need to install the grunt command line utility.  To do this run:

    npm install -g grunt-cli

Test pages are optionally generated using [Jekyll](http://jekyllrb.com/). To use jekyll to build the test pages, ensure Ruby is installed and available then run:

```sh
npm run jekyll
```

or

```sh
gem install bundle
bundle install
```

During the install you may be asked for your password as part of the [Ruby](https://www.ruby-lang.org/en/documentation/installation/) installation process.

Next, set the environment variable PF_PAGE_BUILDER=jekyll.  eg.:
    PF_PAGE_BUILDER=jekyll grunt build

#### Keeping NPM Dependencies Updated

Anytime you pull a new version of PatternFly, make sure you also run

```sh
npm update
```

so you get the latest version of the dependencies specified in package.json.

### Live Reload Server

A local development server can be quickly fired up by using the Gruntjs serve task:

```sh
npm start
```

or

```sh
grunt serve                 # will build first by default
grunt serve --skipRebuild   # flag would allow you to skip the rebuild to save some time
```

This local static asset server (i.e., [http://localhost:9000](http://localhost:9000)) has the advantage of having livereload integration. Thus, if you start the Gruntjs server, any changes you make to `.html` or `.less` files will be automatically reloaded into your browser and the changes reflected almost immediately. This has the obvious benefit of not having to refresh your browser and still be able to see the changes as you add or remove them from your development files.  Additionally, any changes made to Jekyll source files (`tests/pages/`) will trigger a Jekyll build.

### Coding Style

See [http://codeguide.patternfly.org/](http://codeguide.patternfly.org/).

### Commiting changes

PatternFly uses the [semantic-release tool](https://github.com/semantic-release/semantic-release) to provide a continuous release mechanism for PatternFly.  In order for this tool to correctly increment the project version, and include your changes in the generated release notes, you will have to format your commit messages according to a well-defined commit message format.

We have configured the [commitizen tool](https://github.com/commitizen/cz-cli) to assist you in formatting your commit messages corrctly.  To use this tool run the following command instead of `git commit`:

```sh
npm run commit
```

#### Git Commit Guidelines

Alternatively, if you are familiar with the commititzen message format you can format the message manually.  A summary of the commit message format is as follows:

Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject** ([full explanation](https://github.com/stevemao/conventional-changelog-angular/blob/master/convention.md)):

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

##### Patch Release

```
fix(pencil): stop graphite breaking when too much pressure applied
```

##### Feature Release

```
feat(pencil): add 'graphiteWidth' option
```

##### Breaking Release

```
perf(pencil): remove graphiteWidth option
```
The tool will prompt you with several questions that it will use to correctly format your commit message.  You can then proceed with your PR as you normally would.

## Build

### CSS

In development, styling is written and managed through multiple Less files. In order to generate a CSS file of all styling, run the build Gruntjs task:

```sh
npm run build
```

or

```sh
grunt build
```

This task will compile and minify the Less files into CSS files located at `dist/css/patternfly.min.css` and `dist/css/patternfly-additions.min.css`.

### Less to Sass Conversion
Any time style changes are introduced, the Sass code will need to be updated to reflect those changes. The conversion is accomplished as part of the build, but in order to test the CSS you will need to build it from Sass:

```sh
npm start -- --sass
```
*Note the extra ` -- ` between `npm start` and the `--sass` flag. This syntax passes the flag on to the underlying grunt process instead of the npm command itself.*

or

```sh
grunt build --sass
```

These tasks will run a Less to Sass conversion, then compile and minify the resulting Sass into CSS, creating a `dist/css/patternfly.css` file. Note that building from Sass does not create the `dist/css/patternfly-additions.css` file, which is the expected behavior.

The Less to Sass Conversion step will be accomplished and managed as a part of any Pull Request which includes Less file changes. Although contributors may want to build and test their style changes with Sass before submitting a Pull Request, this step should always be tested and validated by reviewers before a style change is merged and released. If a contributor is having issues with Sass conversion that they cannot resolve, Pull Request reviewers will need to ensure that the Sass conversion step is successfully accomplished, tested, and included in the Pull Request before it is approved and merged.

### Scenarios to Avoid when Making Less Style Updates

Sass and Less do not have perfect feature parity, which can sometimes throw a wrench into the conversion process described above. Furthermore, a failed conversion may be somewhat transparent since it may create Sass that will compile to unexpected, but valid CSS. The following are a few known scenarios that can be easily avoided to prevent failures in the Less to Sass conversion process.

#### Non-parametric Mixins
Sass does not support non-parametric mixins in the same way that Less does. Mixins must be explictly declared in Sass, whereas any class definition in Less can be used as a non-parametric mixin. Sass does not have a feature that perfectly parallels this behavior, so we have to use the closest thing which is the `@extend` statement. However, an edge case exists where `@extend` statements are not allowed within media queries in Sass. This creates a scenario where uncompilable Sass code can be generated from perfectly acceptable Less. For example:
**Less:**
```less
  .applauncher-pf {
    .applauncher-pf-title {
        .sr-only();
    }
  }

  @media (min-width: @screen-sm-min) {
   .applauncher-pf;
  }
```

**Converts to Sass:**
```scss
  .applauncher-pf {
    .applauncher-pf-title {
      @extend .sr-only;
    }
  }

  @media (min-width: @screen-sm-min) {
    @extend applauncher-pf; //Invalid Sass
  }
```

This breaks for two reasons. We cannot use the `@extend` statement directly inside a media query, and even if we are able to work around that by making applauncher-pf into a mixin and using the `@include` directive, `.applauncher-pf .applauncher-pf-title` uses the `@extend` directive, which would still fall within the media query via the mixin invocation. To fix this, the Less would need to be adjusted like this:

**Less**
```less
  // Explicitly define a non-parametric sr-only mixin.
  .sr-only() {
    // sr-only rules;
  }

  // Explicitly define a non-parametric applauncher-pf mixin.
  .applauncher-pf() {
    .applauncher-pf-title {
      // Explicitly invoke sr-only mixin.
      .sr-only();
    }
  }

  // Define the .applauncher-pf class and explicitly invoke the applauncher-pf
  // mixin.
  .applauncher-pf {
    .applauncher-pf();
  }

  @media (min-width: @screen-sm-min) {
    // Explicitly invoke applauncher-pf mixin inside of media query
   .applauncher-pf();
  }
```

**Converts to Sass:**
```scss
  @mixin sr-only() {
    // sr-only rules
  }

  @mixin applauncher-pf() {
    .applauncher-pf-title {
      @include sr-only();
    }
  }

  .applauncher-pf {
    @include applauncher-pf();
  }

  @media (min-width: @screen-sm-min) {
    @include applauncher-pf();
  }
```

#### Tilde-Escaped Strings
Strings that are escaped using the tilde in Less get converted to the Sass `unquote()` function. This causes Sass compilation issues when using escaped strings inside native CSS functions like `calc()`. Here is what happens:
Less:
```less
height: calc(~"100vh - 20px");
```
Converts to Sass:
```scss
height: calc(unqoute("100vh - 20px")):
```
Which compiles directly to CSS and does not work as expected:
```css
height: calc(unqoute("100vh - 20px")):
```

To fix this, move the tilde operator outside of the `calc()` statement:

Less:
```less
height: ~"calc(100vh - 20px)";
```
Converts to Sass:
```scss
height: unqoute("calc(100vh - 20px)");
```
Compiles to CSS:
```css
height: calc(100vh - 20px);
```

#### Comma Separated CSS Rules
Using complex, comma separated rules in things like box shadows or backgrounds will cause conversion problems if they are not properly escaped. These rules should be escaped, and mixins and variables should not be used inline. For example, this statement should not be used in Less:
```css
box-shadow: inset 0 1px 1px fade(@color-pf-black, 7.5%), 0 0 6px lighten(@state-danger-text, 20%);
```

Instead, mixins should be assigned to variables, and variables should be interpolated in an escaped string like this:

```scss
@color1: fade(@color-pf-black, 7.5%);
@color2: lighten(@state-danger-text, 20%);
box-shadow: ~"inset 0 1px 1px @{color1}, 0 0 6px @{color2}";
```

This is especially important when passing a complex rule to a mixin.

### PatternFlyIcons Font

PatternFlyIcons font is generated using [IcoMoon](http://icomoon.io/app). Go to [manage projects](https://icomoon.io/app/#/projects) and import the project `PatternFlyIcons-webfont.json`. Load it and update as necessary. When finished, return to manage projects, and download the updated `PatternFlyIcons-webfont.json` file. Also generate the fonts. Please commit the updated `PatternFlyIcons-webfont.json` file, the updated font files and supporting LESS/CSS changes.
For detailed instructions, please see our [PatternFly Icon Guide](PFICONS.md)

## Tests

The `tests/` directory contains HTML pages with component and pattern examples in order to facilitate development.  Please consult the official documentation (see below) for full details on how to use PatternFly.  The latest PatternFly test directory examples can be seen at [https://rawgit.com/patternfly/patternfly/master-dist/dist/tests/](https://rawgit.com/patternfly/patternfly/master-dist/dist/tests/).

If you are developing on PatternFly and would like to provide a link to a test directory from your fork, TravisCI can be configured to create a copy of your branch with the dist files generated for you.  No code changes are necessary to enable this, all that is needed is to login to [TravisCI](https://travis-ci.org/) and configure it to point at your PatternFly fork.  The first three steps at their [Getting Started page](https://docs.travis-ci.com/user/for-beginners) provide instructions on how to do this.  You will also need to add an AUTH_TOKEN variable to Travis generated in your GitHub account to allow Travis to connect to your fork.

The HTML pages in `dist/tests` are generated using Jekyll.  Do *not* edit these files directly.  See `tests/pages` to change these files.

### Unit Testing
Unit tests are written for [Karma test server] (https://karma-runner.github.io/1.0/index.html) with [Jasmine](http://jasmine.github.io/)

```sh
npm test
```

or

```sh
grunt karma
```
### Visual Regression Testing

*Visual regression tests require Jekyll to be installed*

Visual regression tests provide a way to detect if unintended visual changes have
occured as a result of changes in the code. They work by taking screenshots of
what components or pages should look like in a browser (known as references), and then comparing the references to screenshots of those components or pages with your code changes applied. Once the tests are complete, you will be a shown the test results in a browser.

You can run all of the test scenarios with `npm run regressions`.

You can run specific test scenarios with `npm run regressions <scenario-name>`. This
will probably be most useful while you are doing active development and only want
to check a few scenarios without running the entire suite.
(Ex. `npm run regressions alerts buttons`)

To approve conflicts run: `npm run approve-conflicts`. This is the command you want to run
when the tests find conflicts, but the conflicts are intended. This command
will replace the base image, so if you run the regression tests again, the
tests should pass.

## Documentation

See [https://www.patternfly.org](https://www.patternfly.org) and [http://getbootstrap.com/](http://getbootstrap.com/).

### Browser and Device Support

Since PatternFly is based on Bootstrap, PatternFly supports [the same browsers as Bootstrap](http://getbootstrap.com/getting-started/#support) **excluding Internet Explorer 8**, plus the latest version of [Firefox for Linux](https://support.mozilla.org/en-US/kb/install-firefox-linux).

*Important:*  starting with the v2.0.0 release, **PatternFly no longer supports Internet Explorer 8**.

### Product Backlog

See [https://patternfly.atlassian.net/secure/RapidBoard.jspa?projectKey=PTNFLY&rapidView=4&view=planning](https://patternfly.atlassian.net/secure/RapidBoard.jspa?projectKey=PTNFLY&rapidView=4&view=planning).

### Bug List

Official tracking of bugs occurs in Jira.  See https://patternfly.atlassian.net/issues/?filter=10304

## License

Modifications to Bootstrap are copyright 2019 Red Hat, Inc. and licensed under the [MIT License](./LICENSE.txt).
