var $ = require('../internals/export');
var global = require('../internals/global');
var setTimeout = require('../internals/schedulers-fix').setTimeout;

// ie9- setTimeout additional parameters fix
// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
$({ global: true, bind: true, forced: global.setTimeout !== setTimeout }, {
  setTimeout: setTimeout
});
