# Shx

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fshelljs%2Fshx%2Fbadge%3Fref%3Dmaster&style=flat-square)](https://actions-badge.atrox.dev/shelljs/shx/goto?ref=master)
[![Codecov](https://img.shields.io/codecov/c/github/shelljs/shx/master.svg?style=flat-square&label=coverage)](https://codecov.io/gh/shelljs/shx)
[![npm version](https://img.shields.io/npm/v/shx.svg?style=flat-square)](https://www.npmjs.com/package/shx)
[![npm downloads](https://img.shields.io/npm/dm/shx.svg?style=flat-square)](https://www.npmjs.com/package/shx)

`shx` is a wrapper around [ShellJS](https://github.com/shelljs/shelljs) Unix
commands, providing an easy solution for simple Unix-like, cross-platform
commands in npm package scripts.

`shx` is proudly tested on every node release since <!-- start minVersion -->`v6`<!-- stop minVersion -->!

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

**Tip:** because Windows treats single quotes (ex. `'some string'`) differently
than double quotes, [we
recommend](https://github.com/shelljs/shx/issues/165#issuecomment-563127983)
wrapping your arguments in double quotes for cross platform compatibility (ex.
`"some string"`).

## Command reference

Shx exposes [most ShellJS
commands](https://github.com/shelljs/shelljs#command-reference). If a command is
not listed here, assume it's supported!

### sed

Shx provides unix-like syntax on top of `shell.sed()`. So ShellJS code like:

```js
shell.sed('-i', /original string/g, 'replacement', 'filename.txt');
```

would turn into the following Shx command:

```sh
shx sed -i "s/original string/replacement/g" filename.txt
```

**Note:** like unix `sed`, `shx sed` treats `/` as a special character, and
[this must be
escaped](https://github.com/shelljs/shx/issues/169#issuecomment-563013849) (as
`\/` in the shell, or `\\/` in `package.json`) if you intend to use this
character in either the regex or replacement string. Do **not** escape `/`
characters in the file path.

### Unsupported Commands

As mentioned above, most ShellJS commands are supported in ShellJS. Due to the
differences in execution environments between ShellJS and `shx` (JS vs CLI) the
following commands are not supported:

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
