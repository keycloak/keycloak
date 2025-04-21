# @rushstack/eslint-patch

A patch that improves how ESLint loads plugins when working in a monorepo with a reusable toolchain


## What it does

This patch is a workaround for a longstanding [ESLint feature request](https://github.com/eslint/eslint/issues/3458)
that would allow a shared ESLint config to bring along its own plugins, rather than imposing peer dependencies
on every consumer of the config.  In a monorepo scenario, this enables your lint setup to be consolidated in a
single NPM package.  Doing so greatly reduces the copy+pasting and version management for all the other projects
that use your standard lint rule set, but don't want to be bothered with the details.

ESLint provides partial solutions such as the `--resolve-plugins-relative-to` CLI option, however they are
awkward to use.  For example, the VS Code extension for ESLint must be manually configured with this CLI option.
If some developers use other editors such as WebStorm, a different manual configuration is needed.
Also, the `--resolve-plugins-relative-to` parameter does not support multiple paths, for example if a config package
builds upon another package that also provides plugins.  See
[this discussion](https://github.com/eslint/eslint/issues/3458#issuecomment-516666620)
for additional technical background.


## Why it's a patch

ESLint's long awaited module resolver overhaul still has not materialized as of ESLint 8.  As a stopgap,
we created a small **.eslintrc.js** patch that solves the problem adequately for most real world scenarios.
This patch was proposed as an ESLint feature with [PR 12460](https://github.com/eslint/eslint/pull/12460), however
the maintainers were not able to accept it unless it is reworked into a fully correct design.  Such a requirement
would impose the same hurdles as the original GitHub issue; thus, it seems best to stay with the patch approach.

Since the patch is now in wide use, we've converted it into a proper NPM package to simplify maintenance.


## How to use it

Add a `require()` call to the to top of the **.eslintrc.js** file for each project that depends on your shared
ESLint config, for example:

**.eslintrc.js**
```ts
require("@rushstack/eslint-patch/modern-module-resolution");

// Add your "extends" boilerplate here, for example:
module.exports = {
  extends: ['@your-company/eslint-config'],
  parserOptions: { tsconfigRootDir: __dirname }
};
```

With this change, the local project no longer needs any ESLint plugins in its **package.json** file.
Instead, the hypothetical `@your-company/eslint-config` NPM package would declare the plugins as its
own dependencies.

This patch works by modifying the ESLint engine so that its module resolver will load relative to the folder of
the referencing config file, rather than the project folder.  The patch is compatible with ESLint 6, 7, and 8.
It also works with any editor extensions that load ESLint as a library.

For an even leaner setup, `@your-company/eslint-config` can provide the patch as its own dependency.  See
[@rushstack/eslint-config](https://www.npmjs.com/package/@rushstack/eslint-config) for a real world example
and recommended approach.


## Links

- [CHANGELOG.md](https://github.com/microsoft/rushstack/blob/main/eslint/eslint-patch/CHANGELOG.md) - Find
  out what's new in the latest version

`@rushstack/eslint-patch` is part of the [Rush Stack](https://rushstack.io/) family of projects.
