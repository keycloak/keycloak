# opn

> A better [node-open](https://github.com/pwnall/node-open). Opens stuff like websites, files, executables. Cross-platform.


#### Why?

- Actively maintained
- Supports app arguments
- Safer as it uses `spawn` instead of `exec`
- Fixes most of the open `node-open` issues
- Includes the latest [`xdg-open` script](http://cgit.freedesktop.org/xdg/xdg-utils/commit/?id=c55122295c2a480fa721a9614f0e2d42b2949c18) for Linux


## Install

```
$ npm install --save opn
```


## Usage

```js
var opn = require('opn');

opn('unicorn.png');
// opens the image in the default image viewer

opn('http://sindresorhus.com');
// opens the url in the default browser

opn('http://sindresorhus.com', {app: 'firefox'});
// specify the app to open in

opn('http://sindresorhus.com', {app: ['google chrome', '--incognito']});
// specify app arguments
```


## API

Uses the command `open` on OS X, `start` on Windows and `xdg-open` on other platforms.

### opn(target, [options], [callback])

Returns the [spawned child process](https://nodejs.org/api/child_process.html#child_process_class_childprocess). You'd normally not need to use this for anything, but it can be useful if you'd like to attach custom event listeners or perform other operations directly on the spawned process.

#### target

*Required*  
Type: `string`

The thing you want to open. Can be a URL, file, or executable.

Opens in the default app for the file type. Eg. URLs opens in your default browser.

#### options

Type: `object`

##### wait

Type: `boolean`  
Default: `true`

Wait for the opened app to exit before calling the `callback`. If `false` it's called immediately when opening the app.

On Windows you have to explicitly specify an app for it to be able to wait.

##### app

Type: `string`, `array`

Specify the app to open the `target` with, or an array with the app and app arguments.

The app name is platform dependent. Don't hard code it in reusable modules. Eg. Chrome is `google chrome` on OS X, `google-chrome` on Linux and `chrome` on Windows.

#### callback(error)

Type: `function`

Called when the opened app exits, or if `wait: false`, immediately when opening.


## Related

- [opn-cli](https://github.com/sindresorhus/opn-cli) - CLI for this module


## License

MIT © [Sindre Sorhus](http://sindresorhus.com)
