# Shx

[![Travis](https://img.shields.io/travis/shelljs/shx/master.svg?style=flat-square&label=unix)](https://travis-ci.org/shelljs/shx)
[![AppVeyor](https://img.shields.io/appveyor/ci/shelljs/shx/master.svg?style=flat-square&label=windows)](https://ci.appveyor.com/project/shelljs/shx/branch/master)
[![Codecov](https://img.shields.io/codecov/c/github/shelljs/shx/master.svg?style=flat-square&label=coverage)](https://codecov.io/gh/shelljs/shx)
[![npm version](https://img.shields.io/npm/v/shx.svg?style=flat-square)](https://www.npmjs.com/package/shx)
[![npm downloads](https://img.shields.io/npm/dm/shx.svg?style=flat-square)](https://www.npmjs.com/package/shx)

`shx` is a wrapper around [ShellJS](https://github.com/shelljs/shelljs) Unix
commands, providing an easy solution for simple Unix-like, cross-platform
commands in npm package scripts.

## Difference Between ShellJS and shx

- **ShellJS:** Good for writing long scripts, all in JS, running via NodeJS (e.g. `node myScript.js`).
- **shx:** Good for writing one-off commands in npm package scripts (e.g. `"clean": "shx rm -rf out/"`).

## Install

```shell
npm install shx --save-dev
```
This will allow using `shx` in your `package.json` scripts.

## Usage

### Command Line

If you'd like to use `shx` on the command line, install it globally with the `-g` flag.
The following code can be run *either a Unix or Windows* command line:

```Bash
$ shx pwd                       # ShellJS commands are supported automatically
/home/username/path/to/dir

$ shx ls                        # files are outputted one per line
file.txt
file2.txt

$ shx rm *.txt                  # a cross-platform way to delete files!

$ shx ls

$ shx echo "Hi there!"
Hi there!

$ shx touch helloworld.txt

$ shx cp helloworld.txt foobar.txt

$ shx mkdir sub

$ shx ls
foobar.txt
helloworld.txt
sub

$ shx rm -r sub                 # options work as well

$ shx --silent ls fakeFileName  # silence error output
```

All commands internally call the ShellJS corresponding function, guaranteeing
cross-platform compatibility.

### package.json

ShellJS is good for writing long scripts. If you want to write bash-like,
platform-independent scripts, we recommend you go with that.

However, `shx` is ideal for one-liners inside `package.json`:

```javascript
{
  "scripts": {
    "clean": "shx rm -rf build dist && shx echo Done"
  }
}
```

## Unsupported Commands

Due to the differences in execution environments between ShellJS and `shx` (JS vs CLI) some commands are not supported:

| Unsupported command | Recommend workaround |
| ------------------- | -------------------- |
| `shx cd`            | Just use plain old `cd` (it's the same on windows too) |
| `shx pushd`         | Just use plain old `pushd`. Use forward slashes and double-quote the path. (e.g. `pushd "../docs"`. This would fail on Windows without the quotes) |
| `shx popd`          | Just use plain old `popd` |
| `shx dirs`          | No workaround |
| `shx set`           | See below |
| `shx exit`          | Just use plain old `exit` |
| `shx exec`          | Instead of `shx exec cmd`, just use plain old `cmd` |
| `shx ShellString`   | No workaround (but why would you want this?) |

### Shx options

Shx allows you to modify its behavior by passing arguments. Here's a list of
supported options:

| [`set`](https://github.com/shelljs/shelljs#setoptions) flag | [`shell.config`](https://github.com/shelljs/shelljs#configuration) setting | shx command | Effect |
|:---:| --- | --- | --- |
| `-e` | `config.fatal = true` | Not supported | Exit upon first error |
| `-v` | `config.verbose = true` | `shx --verbose cd foo` | Log the command as it's run |
| `-f` | `config.noglob = true` | `shx --noglob cat '*.txt'` | Don't expand wildcards |
| N/A | `config.silent = true` | `shx --silent cd noexist` | Don't show error output |

## Team

| [![Nate Fischer](https://avatars.githubusercontent.com/u/5801521?s=130)](https://github.com/nfischer) | [![Ari Porad](https://avatars1.githubusercontent.com/u/1817508?v=3&s=130)](http://github.com/ariporad) | [![Levi Thomason](https://avatars1.githubusercontent.com/u/5067638?v=3&s=130)](https://github.com/levithomason) |
|:---:|:---:|:---:|
| [Nate Fischer](https://github.com/nfischer) | [Ari Porad](http://github.com/ariporad) | [Levi Thomason](https://github.com/levithomason) |
