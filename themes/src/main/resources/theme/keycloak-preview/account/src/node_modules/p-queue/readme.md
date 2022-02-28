# p-queue [![Build Status](https://travis-ci.org/sindresorhus/p-queue.svg?branch=master)](https://travis-ci.org/sindresorhus/p-queue)

> Promise queue with concurrency control

Useful for rate-limiting async (or sync) operations. For example, when interacting with a REST API or when doing CPU/memory intensive tasks.


## Install

```
$ npm install p-queue
```


## Usage

Here we run only one promise at the time. For example, set `concurrency` to 4 to run four promises at the time.

```js
const PQueue = require('p-queue');
const got = require('got');

const queue = new PQueue({concurrency: 1});

queue.add(() => got('sindresorhus.com')).then(() => {
	console.log('Done: sindresorhus.com');
});

queue.add(() => got('ava.li')).then(() => {
	console.log('Done: ava.li');
});

getUnicornTask().then(task => queue.add(task)).then(() => {
	console.log('Done: Unicorn task');
});
```


## API

### PQueue([options])

Returns a new `queue` instance.

#### options

Type: `Object`

##### concurrency

Type: `number`<br>
Default: `Infinity`<br>
Minimum: `1`

Concurrency limit.

##### autoStart

Type: `boolean`<br>
Default: `true`

Whether queue tasks within concurrency limit, are auto-executed as soon as they're added.

##### queueClass

Type: `Function`

Class with a `enqueue` and `dequeue` method, and a `size` getter. See the [Custom QueueClass](#custom-queueclass) section.

### queue

`PQueue` instance.

#### .add(fn, [options])

Adds a sync or async task to the queue. Always returns a promise.

##### fn

Type: `Function`

Promise-returning/async function.

#### options

Type: `Object`

##### priority

Type: `number`<br>
Default: `0`

Priority of operation. Operations with greater priority will be scheduled first.

#### .addAll(fns, [options])

Same as `.add()`, but accepts an array of sync or async functions and returns a promise that resolves when all functions are resolved.

#### .pause()

Put queue execution on hold.

#### .start()

Start (or resume) executing enqueued tasks within concurrency limit. No need to call this if queue is not paused (via `options.autoStart = false` or by `.pause()` method.)

#### .onEmpty()

Returns a promise that settles when the queue becomes empty.

Can be called multiple times. Useful if you for example add additional items at a later time.

#### .onIdle()

Returns a promise that settles when the queue becomes empty, and all promises have completed; `queue.size === 0 && queue.pending === 0`.

The difference with `.onEmpty` is that `.onIdle` guarantees that all work from the queue has finished. `.onEmpty` merely signals that the queue is empty, but it could mean that some promises haven't completed yet.

#### .clear()

Clear the queue.

#### .size

Size of the queue.

#### .pending

Number of pending promises.

#### .isPaused

Whether the queue is currently paused.

## Advanced example

A more advanced example to help you understand the flow.

```js
const delay = require('delay');
const PQueue = require('p-queue');

const queue = new PQueue({concurrency: 1});

delay(200).then(() => {
	console.log(`8. Pending promises: ${queue.pending}`);
	//=> '8. Pending promises: 0'

	queue.add(() => Promise.resolve('🐙')).then(console.log.bind(null, '11. Resolved'));

	console.log('9. Added 🐙');

	console.log(`10. Pending promises: ${queue.pending}`);
	//=> '10. Pending promises: 1'

	queue.onIdle().then(() => {
		console.log('12. All work is done');
	});
});

queue.add(() => Promise.resolve('🦄')).then(console.log.bind(null, '5. Resolved'));
console.log('1. Added 🦄');

queue.add(() => Promise.resolve('🐴')).then(console.log.bind(null, '6. Resolved'));
console.log('2. Added 🐴');

queue.onEmpty().then(() => {
	console.log('7. Queue is empty');
});

console.log(`3. Queue size: ${queue.size}`);
//=> '3. Queue size: 1`
console.log(`4. Pending promises: ${queue.pending}`);
//=> '4. Pending promises: 1'
```

```
$ node example.js
1. Added 🦄
2. Added 🐴
3. Queue size: 1
4. Pending promises: 1
5. Resolved 🦄
6. Resolved 🐴
7. Queue is empty
8. Pending promises: 0
9. Added 🐙
10. Pending promises: 1
11. Resolved 🐙
12. All work is done
```


## Custom QueueClass

For implementing more complex scheduling policies, you can provide a QueueClass in the options:

```js
class QueueClass {
	constructor() {
		this._queue = [];
	}
	enqueue(run, options) {
		this._queue.push(run);
	}
	dequeue() {
		return this._queue.shift();
	}
	get size() {
		return this._queue.length;
	}
}
```

`p-queue` will call corresponding methods to put and get operations from this queue.


## Related

- [p-limit](https://github.com/sindresorhus/p-limit) - Run multiple promise-returning & async functions with limited concurrency
- [p-throttle](https://github.com/sindresorhus/p-throttle) - Throttle promise-returning & async functions
- [p-debounce](https://github.com/sindresorhus/p-debounce) - Debounce promise-returning & async functions
- [p-all](https://github.com/sindresorhus/p-all) - Run promise-returning & async functions concurrently with optional limited concurrency
- [More…](https://github.com/sindresorhus/promise-fun)


## Created by

- [Sindre Sorhus](https://github.com/sindresorhus)
- [Vsevolod Strukchinsky](https://github.com/floatdrop)


## License

MIT © [Sindre Sorhus](https://sindresorhus.com)
