# Change Log

All notable changes to this project will be documented in this file.
See [Conventional Commits](https://conventionalcommits.org) for commit guidelines.

## [1.4.2](https://github.com/typescript-eslint/typescript-eslint/compare/v1.4.1...v1.4.2) (2019-02-25)

**Note:** Version bump only for package @typescript-eslint/typescript-estree

## [1.4.1](https://github.com/typescript-eslint/typescript-eslint/compare/v1.4.0...v1.4.1) (2019-02-23)

**Note:** Version bump only for package @typescript-eslint/typescript-estree

# [1.4.0](https://github.com/typescript-eslint/typescript-eslint/compare/v1.3.0...v1.4.0) (2019-02-19)

### Bug Fixes

- **ts-estree:** make sure that every node can be converted to tsNode ([#287](https://github.com/typescript-eslint/typescript-eslint/issues/287)) ([9f1d314](https://github.com/typescript-eslint/typescript-eslint/commit/9f1d314))
- **typescript-estree, eslint-plugin:** stop adding ParenthesizedExpressions to node maps ([#226](https://github.com/typescript-eslint/typescript-eslint/issues/226)) ([317405a](https://github.com/typescript-eslint/typescript-eslint/commit/317405a))

### Features

- **eslint-plugin:** add 'no-unnecessary-qualifier' rule ([#231](https://github.com/typescript-eslint/typescript-eslint/issues/231)) ([cc8f906](https://github.com/typescript-eslint/typescript-eslint/commit/cc8f906))
- **eslint-plugin:** Migrate plugin to ts ([#120](https://github.com/typescript-eslint/typescript-eslint/issues/120)) ([61c60dc](https://github.com/typescript-eslint/typescript-eslint/commit/61c60dc))
- **ts-estree:** fix parsing nested sequence expressions ([#286](https://github.com/typescript-eslint/typescript-eslint/issues/286)) ([ecc9631](https://github.com/typescript-eslint/typescript-eslint/commit/ecc9631))

# [1.3.0](https://github.com/typescript-eslint/typescript-eslint/compare/v1.2.0...v1.3.0) (2019-02-07)

### Bug Fixes

- **ts-estree:** align typeArguments and typeParameters across nodes ([#223](https://github.com/typescript-eslint/typescript-eslint/issues/223)) ([3306198](https://github.com/typescript-eslint/typescript-eslint/commit/3306198))
- **ts-estree:** convert decorators on var and fn decs ([#211](https://github.com/typescript-eslint/typescript-eslint/issues/211)) ([0a1777f](https://github.com/typescript-eslint/typescript-eslint/commit/0a1777f))
- **ts-estree:** fix issues with typeParams in FunctionExpression ([#208](https://github.com/typescript-eslint/typescript-eslint/issues/208)) ([d4dfa3b](https://github.com/typescript-eslint/typescript-eslint/commit/d4dfa3b))

### Features

- change TypeScript version range to >=3.2.1 <3.4.0 ([#184](https://github.com/typescript-eslint/typescript-eslint/issues/184)) ([f513a14](https://github.com/typescript-eslint/typescript-eslint/commit/f513a14))
- **ts-estree:** enable errors 1098 and 1099 ([#219](https://github.com/typescript-eslint/typescript-eslint/issues/219)) ([fc50167](https://github.com/typescript-eslint/typescript-eslint/commit/fc50167))

# [1.2.0](https://github.com/typescript-eslint/typescript-eslint/compare/v1.1.1...v1.2.0) (2019-02-01)

**Note:** Version bump only for package @typescript-eslint/typescript-estree

## [1.1.1](https://github.com/typescript-eslint/typescript-eslint/compare/v1.1.0...v1.1.1) (2019-01-29)

### Bug Fixes

- **parser:** add visiting of type parameters in JSXOpeningElement ([#150](https://github.com/typescript-eslint/typescript-eslint/issues/150)) ([5e16003](https://github.com/typescript-eslint/typescript-eslint/commit/5e16003))
- **ts-estree:** expand optional property to include question token ([#138](https://github.com/typescript-eslint/typescript-eslint/issues/138)) ([9068b62](https://github.com/typescript-eslint/typescript-eslint/commit/9068b62))

### Performance Improvements

- **ts-estree:** don't create Program in parse() ([#148](https://github.com/typescript-eslint/typescript-eslint/issues/148)) ([aacf5b0](https://github.com/typescript-eslint/typescript-eslint/commit/aacf5b0))

# [1.1.0](https://github.com/typescript-eslint/typescript-eslint/compare/v1.0.0...v1.1.0) (2019-01-23)

### Bug Fixes

- **typescript-estree:** correct range of parameters with comments ([#128](https://github.com/typescript-eslint/typescript-eslint/issues/128)) ([91eedf2](https://github.com/typescript-eslint/typescript-eslint/commit/91eedf2))
- **typescript-estree:** fix range of assignment in parameter ([#115](https://github.com/typescript-eslint/typescript-eslint/issues/115)) ([4e781f1](https://github.com/typescript-eslint/typescript-eslint/commit/4e781f1))

# [1.0.0](https://github.com/typescript-eslint/typescript-eslint/compare/v0.2.1...v1.0.0) (2019-01-20)

### Features

- **parser:** support ecmaFeatures.jsx flag and tests ([#85](https://github.com/typescript-eslint/typescript-eslint/issues/85)) ([b321736](https://github.com/typescript-eslint/typescript-eslint/commit/b321736))

## [0.2.1](https://github.com/typescript-eslint/typescript-eslint/compare/v0.2.0...v0.2.1) (2019-01-20)

**Note:** Version bump only for package @typescript-eslint/typescript-estree
