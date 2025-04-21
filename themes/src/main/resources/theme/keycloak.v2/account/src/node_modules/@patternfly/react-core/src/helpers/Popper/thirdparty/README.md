# react-popper
Copied and modified from https://github.com/popperjs/react-popper ("react-popper": "2.2.3")

# popper-core
Copied and modified from https://github.com/popperjs/popper-core ("@popperjs/core": "2.4.2")

## Why
Brought this in so that consumers don't have to deal with potential build errors / jest errors, etc

## Note
The code may not have the same level of quality as other components since it is thirdparty code that was modified just enough to make it work with the build. Although some types and eslint issues were fixed, many files have TS errors that were suppressed with the `// @ts-nocheck` comment, and some eslint errors were also supressed with `/* eslint-disable SOMETHING /*` comments.

## Some modifications to make it work with our build
- Converted flow to typescript (using npm package flow-to-ts)
- Fixed some ts linting issues/added eslint ignores and ts-nocheck comments
- Copied in some util functions
- Some source changes
- Replaced some dependencies (import isEqual from 'react-fast-compare' => JSON.stringify)

# Changes for next breaking-change release
- Delete the thirdparty folder and add them as dependencies to the package.json:
`"react-popper": "2.2.3"`
`"@popperjs/core": "2.4.2"`