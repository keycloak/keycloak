# Change Log

## [Unreleased](https://github.com/shelljs/shx/tree/HEAD)

[Full Changelog](https://github.com/shelljs/shx/compare/v0.3.1...HEAD)

**Closed issues:**

- Mocha and babel compiler warnings [\#144](https://github.com/shelljs/shx/issues/144)
- Add "true" and "false" commands [\#105](https://github.com/shelljs/shx/issues/105)
- --help should list options [\#101](https://github.com/shelljs/shx/issues/101)

**Merged pull requests:**

- chore: fix issues with nyc and mocha [\#145](https://github.com/shelljs/shx/pull/145) ([nfischer](https://github.com/nfischer))
- feat: help command provides info on options [\#143](https://github.com/shelljs/shx/pull/143) ([nfischer](https://github.com/nfischer))
- feat: add true and false as default commands [\#142](https://github.com/shelljs/shx/pull/142) ([nfischer](https://github.com/nfischer))

## [v0.3.1](https://github.com/shelljs/shx/tree/v0.3.1) (2018-06-26)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.3.0...v0.3.1)

**Closed issues:**

- curl / wget [\#141](https://github.com/shelljs/shx/issues/141)
- Regex support for forward slashes in sed [\#136](https://github.com/shelljs/shx/issues/136)
- Appveyor config broken for latest npm [\#132](https://github.com/shelljs/shx/issues/132)
- `shx echo -n aaa` shows `-n aaa` [\#129](https://github.com/shelljs/shx/issues/129)
- feature request: --version [\#111](https://github.com/shelljs/shx/issues/111)

**Merged pull requests:**

- test\(coverage\): fix coverage issues [\#139](https://github.com/shelljs/shx/pull/139) ([nfischer](https://github.com/nfischer))
- feat\(version\): support --version flag [\#138](https://github.com/shelljs/shx/pull/138) ([nfischer](https://github.com/nfischer))
- fix\(sed\): better support for converting sed patterns [\#137](https://github.com/shelljs/shx/pull/137) ([nfischer](https://github.com/nfischer))
- chore: bump dev dependencies to resolve install warnings [\#135](https://github.com/shelljs/shx/pull/135) ([nfischer](https://github.com/nfischer))
- chore\(appveyor\): do not use latest npm [\#133](https://github.com/shelljs/shx/pull/133) ([nfischer](https://github.com/nfischer))
- chore\(CI\): add node v10 to CI [\#131](https://github.com/shelljs/shx/pull/131) ([nfischer](https://github.com/nfischer))

## [v0.3.0](https://github.com/shelljs/shx/tree/v0.3.0) (2018-06-08)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.2.2...v0.3.0)

**Closed issues:**

- Ship shx with shelljs@v0.8 [\#125](https://github.com/shelljs/shx/issues/125)
- Support executables [\#124](https://github.com/shelljs/shx/issues/124)
- Discuss: popd/pushd workarounds [\#121](https://github.com/shelljs/shx/issues/121)
- Feature request: date [\#120](https://github.com/shelljs/shx/issues/120)
- Fails trying to utilise the new script-shell npm config [\#117](https://github.com/shelljs/shx/issues/117)
- Refactor: consider using process.exitCode [\#116](https://github.com/shelljs/shx/issues/116)
- Add node v8 to CI [\#114](https://github.com/shelljs/shx/issues/114)
- can we use this without the `shx` prefix? [\#113](https://github.com/shelljs/shx/issues/113)
- Command substitution feature [\#112](https://github.com/shelljs/shx/issues/112)

**Merged pull requests:**

- feat: bump shelljs to expose new features [\#126](https://github.com/shelljs/shx/pull/126) ([nfischer](https://github.com/nfischer))
- fix: use process.exitCode to wait for streams [\#123](https://github.com/shelljs/shx/pull/123) ([nfischer](https://github.com/nfischer))
- Add workaround for pushd and popd to README [\#122](https://github.com/shelljs/shx/pull/122) ([eteeselink](https://github.com/eteeselink))
- Add node 8 to CI [\#115](https://github.com/shelljs/shx/pull/115) ([freitagbr](https://github.com/freitagbr))

## [v0.2.2](https://github.com/shelljs/shx/tree/v0.2.2) (2017-01-08)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.2.1...v0.2.2)

**Closed issues:**

- sed does not accept a blank replacement string [\#109](https://github.com/shelljs/shx/issues/109)
- chore: remove lgtm.co [\#102](https://github.com/shelljs/shx/issues/102)
- chore: consider allowing iojs failures on travis [\#99](https://github.com/shelljs/shx/issues/99)
- chore: switch appveyor to my account [\#97](https://github.com/shelljs/shx/issues/97)

**Merged pull requests:**

- fix: better regex support for sed [\#110](https://github.com/shelljs/shx/pull/110) ([nfischer](https://github.com/nfischer))
- chore: switch appveyor to shelljs account [\#107](https://github.com/shelljs/shx/pull/107) ([nfischer](https://github.com/nfischer))
- Remove lgtm.co related files [\#106](https://github.com/shelljs/shx/pull/106) ([freitagbr](https://github.com/freitagbr))
- Update README.md [\#104](https://github.com/shelljs/shx/pull/104) ([corysimmons](https://github.com/corysimmons))
- chore: allow Travis failures on iojs [\#100](https://github.com/shelljs/shx/pull/100) ([nfischer](https://github.com/nfischer))

## [v0.2.1](https://github.com/shelljs/shx/tree/v0.2.1) (2016-11-19)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.2.0...v0.2.1)

**Closed issues:**

- "shx cp" does not finish on Unix-Systems [\#95](https://github.com/shelljs/shx/issues/95)

**Merged pull requests:**

- fix: only wait for stdin when appropriate [\#98](https://github.com/shelljs/shx/pull/98) ([nfischer](https://github.com/nfischer))
- chore: fix spelling error [\#96](https://github.com/shelljs/shx/pull/96) ([nfischer](https://github.com/nfischer))

## [v0.2.0](https://github.com/shelljs/shx/tree/v0.2.0) (2016-11-08)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.1.4...v0.2.0)

**Closed issues:**

- cp -r fails after 72 directories \(Many files\) [\#94](https://github.com/shelljs/shx/issues/94)
- chore: add node v7 to CI [\#90](https://github.com/shelljs/shx/issues/90)
- Unable to use in postinstall script on Windows [\#88](https://github.com/shelljs/shx/issues/88)
- fs.existsSync is un-deprecated [\#87](https://github.com/shelljs/shx/issues/87)
- piping from cat seems broken [\#85](https://github.com/shelljs/shx/issues/85)
- List usb devices [\#84](https://github.com/shelljs/shx/issues/84)
- Feature request: generic OR [\#83](https://github.com/shelljs/shx/issues/83)
- Add -u flag  support for cp   [\#82](https://github.com/shelljs/shx/issues/82)
- Commands should accept stdin [\#80](https://github.com/shelljs/shx/issues/80)
- Add plugin support [\#73](https://github.com/shelljs/shx/issues/73)

**Merged pull requests:**

- Add node v7 to CI [\#93](https://github.com/shelljs/shx/pull/93) ([freitagbr](https://github.com/freitagbr))
- refactor: various changes [\#92](https://github.com/shelljs/shx/pull/92) ([nfischer](https://github.com/nfischer))
- refactor: use fs.existsSync [\#91](https://github.com/shelljs/shx/pull/91) ([nfischer](https://github.com/nfischer))
- feat: pass stdin along to ShellJS commands [\#89](https://github.com/shelljs/shx/pull/89) ([nfischer](https://github.com/nfischer))
- fix: don't exit until all output is flushed [\#86](https://github.com/shelljs/shx/pull/86) ([nfischer](https://github.com/nfischer))
- chore: add support for v0.11+ [\#81](https://github.com/shelljs/shx/pull/81) ([nfischer](https://github.com/nfischer))
- chore: add npm downloads per month [\#79](https://github.com/shelljs/shx/pull/79) ([nfischer](https://github.com/nfischer))
- feat: support for ShellJS plugins [\#78](https://github.com/shelljs/shx/pull/78) ([nfischer](https://github.com/nfischer))

## [v0.1.4](https://github.com/shelljs/shx/tree/v0.1.4) (2016-08-18)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.1.3...v0.1.4)

**Merged pull requests:**

- fix: sed works with slashes in regex [\#77](https://github.com/shelljs/shx/pull/77) ([nfischer](https://github.com/nfischer))
- test: add unit tests for grep and chmod [\#76](https://github.com/shelljs/shx/pull/76) ([nfischer](https://github.com/nfischer))

## [v0.1.3](https://github.com/shelljs/shx/tree/v0.1.3) (2016-08-10)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.1.2...v0.1.3)

**Closed issues:**

- Using sed [\#74](https://github.com/shelljs/shx/issues/74)
- cp -n should not produce errors if file exists [\#67](https://github.com/shelljs/shx/issues/67)
- Generic NUL output redirect [\#61](https://github.com/shelljs/shx/issues/61)
- Consider using publish-please [\#60](https://github.com/shelljs/shx/issues/60)
- Explain unsupported commands and give workarounds [\#59](https://github.com/shelljs/shx/issues/59)
- shx with no args: help message? [\#48](https://github.com/shelljs/shx/issues/48)
- Setup changelog [\#18](https://github.com/shelljs/shx/issues/18)

**Merged pull requests:**

- fix: allow sed to use a unix syntax [\#75](https://github.com/shelljs/shx/pull/75) ([nfischer](https://github.com/nfischer))
- docs: add workarounds for unsupported commands [\#72](https://github.com/shelljs/shx/pull/72) ([nfischer](https://github.com/nfischer))
- chore: remove unnecessary dependency [\#71](https://github.com/shelljs/shx/pull/71) ([nfischer](https://github.com/nfischer))
- docs: add npm README badge [\#70](https://github.com/shelljs/shx/pull/70) ([nfischer](https://github.com/nfischer))
- docs: unsupported commands [\#69](https://github.com/shelljs/shx/pull/69) ([levithomason](https://github.com/levithomason))
- chore: update shelljs-release [\#66](https://github.com/shelljs/shx/pull/66) ([nfischer](https://github.com/nfischer))
- chore: switch to shelljs-changelog [\#64](https://github.com/shelljs/shx/pull/64) ([nfischer](https://github.com/nfischer))
- chore: switch to shelljs-release [\#63](https://github.com/shelljs/shx/pull/63) ([nfischer](https://github.com/nfischer))
- feat: --silent will silence error output [\#62](https://github.com/shelljs/shx/pull/62) ([nfischer](https://github.com/nfischer))
- Update eslint to version 2.10.1 🚀 [\#57](https://github.com/shelljs/shx/pull/57) ([greenkeeperio-bot](https://github.com/greenkeeperio-bot))
- concurrently@2.1.0 untested ⚠️ [\#56](https://github.com/shelljs/shx/pull/56) ([greenkeeperio-bot](https://github.com/greenkeeperio-bot))
- Update all dependencies 🌴 [\#54](https://github.com/shelljs/shx/pull/54) ([greenkeeperio-bot](https://github.com/greenkeeperio-bot))
- chore\(changelog\): create and add script [\#53](https://github.com/shelljs/shx/pull/53) ([levithomason](https://github.com/levithomason))

## [v0.1.2](https://github.com/shelljs/shx/tree/v0.1.2) (2016-05-11)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.1.1...v0.1.2)

**Closed issues:**

- Code coverage badge seems to show for latest \(passing\) PR, not for master branch [\#51](https://github.com/shelljs/shx/issues/51)

**Merged pull requests:**

- Allow npm owner releases [\#50](https://github.com/shelljs/shx/pull/50) ([levithomason](https://github.com/levithomason))
- feat\(help\): add help command [\#49](https://github.com/shelljs/shx/pull/49) ([ariporad](https://github.com/ariporad))
- Add codecov badge and switch to flat styles [\#47](https://github.com/shelljs/shx/pull/47) ([levithomason](https://github.com/levithomason))
- test: improve test coverage [\#46](https://github.com/shelljs/shx/pull/46) ([nfischer](https://github.com/nfischer))
- refactor: remove babel-polyfill dep [\#45](https://github.com/shelljs/shx/pull/45) ([nfischer](https://github.com/nfischer))
- Add install instructions [\#42](https://github.com/shelljs/shx/pull/42) ([levithomason](https://github.com/levithomason))

## [v0.1.1](https://github.com/shelljs/shx/tree/v0.1.1) (2016-05-03)
[Full Changelog](https://github.com/shelljs/shx/compare/v0.1.0...v0.1.1)

**Closed issues:**

- Broken release: missing runtime dependencies [\#43](https://github.com/shelljs/shx/issues/43)

**Merged pull requests:**

- fix\(deps\): add babel-polyfill as a runtime dep [\#44](https://github.com/shelljs/shx/pull/44) ([nfischer](https://github.com/nfischer))

## [v0.1.0](https://github.com/shelljs/shx/tree/v0.1.0) (2016-05-03)
**Closed issues:**

- Linter checks the code coverage directory [\#36](https://github.com/shelljs/shx/issues/36)
- Codecov.io repo settings [\#35](https://github.com/shelljs/shx/issues/35)
- No rimraf command [\#27](https://github.com/shelljs/shx/issues/27)
- Unable to install with npm [\#24](https://github.com/shelljs/shx/issues/24)
- Switch to Mocha [\#22](https://github.com/shelljs/shx/issues/22)
- Setup coverage [\#19](https://github.com/shelljs/shx/issues/19)
- Setup Travis [\#17](https://github.com/shelljs/shx/issues/17)
- Unable to install as a dependency [\#16](https://github.com/shelljs/shx/issues/16)
- Switch this to use shelljs as a git submodule \(for now\) [\#13](https://github.com/shelljs/shx/issues/13)
- Consider shx REPL [\#12](https://github.com/shelljs/shx/issues/12)
- cd can't work, so we should output a warning [\#6](https://github.com/shelljs/shx/issues/6)
- echo outputs twice [\#5](https://github.com/shelljs/shx/issues/5)
- Initial Discussion and TODOs for 1st release [\#1](https://github.com/shelljs/shx/issues/1)

**Merged pull requests:**

- Initial release [\#41](https://github.com/shelljs/shx/pull/41) ([levithomason](https://github.com/levithomason))
- chore\(CI\): update to node v6 [\#39](https://github.com/shelljs/shx/pull/39) ([nfischer](https://github.com/nfischer))
- Show test coverage on every run [\#38](https://github.com/shelljs/shx/pull/38) ([levithomason](https://github.com/levithomason))
- chore\(lint\): don't lint coverage results [\#37](https://github.com/shelljs/shx/pull/37) ([ariporad](https://github.com/ariporad))
- chore: update shelljs dependency [\#34](https://github.com/shelljs/shx/pull/34) ([nfischer](https://github.com/nfischer))
- chore\(package.json\): add test coverage script [\#33](https://github.com/shelljs/shx/pull/33) ([kwonoj](https://github.com/kwonoj))
- docs: update example in README [\#32](https://github.com/shelljs/shx/pull/32) ([nfischer](https://github.com/nfischer))
- Switch to mocha [\#31](https://github.com/shelljs/shx/pull/31) ([levithomason](https://github.com/levithomason))
- chore: add "Team" section to README [\#30](https://github.com/shelljs/shx/pull/30) ([nfischer](https://github.com/nfischer))
- Add missing rimraf devDependency [\#28](https://github.com/shelljs/shx/pull/28) ([levithomason](https://github.com/levithomason))
- Build on prepublish [\#25](https://github.com/shelljs/shx/pull/25) ([levithomason](https://github.com/levithomason))
- chore\(CI\): add CI badges to README.md [\#23](https://github.com/shelljs/shx/pull/23) ([ariporad](https://github.com/ariporad))
- fix: trailing newlines are now consistent [\#21](https://github.com/shelljs/shx/pull/21) ([nfischer](https://github.com/nfischer))
- fix: fixed import statement [\#20](https://github.com/shelljs/shx/pull/20) ([scott113341](https://github.com/scott113341))
- Use ShellJS GitHub dependency [\#15](https://github.com/shelljs/shx/pull/15) ([levithomason](https://github.com/levithomason))
- Setup tests [\#14](https://github.com/shelljs/shx/pull/14) ([levithomason](https://github.com/levithomason))
- Refactor to ES6 [\#10](https://github.com/shelljs/shx/pull/10) ([levithomason](https://github.com/levithomason))
- Fix file locations and references [\#9](https://github.com/shelljs/shx/pull/9) ([levithomason](https://github.com/levithomason))
- fix: blacklist certain commands [\#8](https://github.com/shelljs/shx/pull/8) ([nfischer](https://github.com/nfischer))
- fix\(echo\): fix \#5 [\#7](https://github.com/shelljs/shx/pull/7) ([nfischer](https://github.com/nfischer))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*