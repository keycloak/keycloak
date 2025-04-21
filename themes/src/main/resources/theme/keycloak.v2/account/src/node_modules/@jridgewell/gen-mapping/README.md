# @jridgewell/gen-mapping

> Generate source maps

`gen-mapping` allows you to generate a source map during transpilation or minification.
With a source map, you're able to trace the original location in the source file, either in Chrome's
DevTools or using a library like [`@jridgewell/trace-mapping`][trace-mapping].

You may already be familiar with the [`source-map`][source-map] package's `SourceMapGenerator`. This
provides the same `addMapping` and `setSourceContent` API.

## Installation

```sh
npm install @jridgewell/gen-mapping
```

## Usage

```typescript
import { GenMapping, addMapping, setSourceContent, encodedMap } from '@jridgewell/gen-mapping';

const map = new GenMapping({
  file: 'output.js',
  sourceRoot: 'https://example.com/',
});

setSourceContent(map, 'input.js', `function foo() {}`);

addMapping(map, {
  // Lines start at line 1, columns at column 0.
  generated: { line: 1, column: 0 },
  source: 'input.js',
  original: { line: 1, column: 0 },
});

addMapping(map, {
  generated: { line: 1, column: 9 },
  source: 'input.js',
  original: { line: 1, column: 9 },
  name: 'foo',
});

assert.deepEqual(encodedMap(map), {
  version: 3,
  file: 'output.js',
  names: ['foo'],
  sourceRoot: 'https://example.com/',
  sources: ['input.js'],
  sourcesContent: ['function foo() {}'],
  mappings: 'AAAA,SAASA',
});
```

## Benchmarks

```
node v18.0.0

amp.js.map
gen-mapping:      addSegment x 462 ops/sec ±1.53% (91 runs sampled)
gen-mapping:      addMapping x 471 ops/sec ±0.77% (93 runs sampled)
source-map-js:    addMapping x 178 ops/sec ±1.14% (84 runs sampled)
source-map-0.6.1: addMapping x 178 ops/sec ±1.21% (84 runs sampled)
source-map-0.8.0: addMapping x 177 ops/sec ±1.21% (83 runs sampled)
Fastest is gen-mapping:      addMapping,gen-mapping:      addSegment

gen-mapping:      decoded output x 157,499,812 ops/sec ±0.25% (100 runs sampled)
gen-mapping:      encoded output x 625 ops/sec ±1.95% (88 runs sampled)
source-map-js:    encoded output x 162 ops/sec ±0.37% (84 runs sampled)
source-map-0.6.1: encoded output x 161 ops/sec ±0.51% (84 runs sampled)
source-map-0.8.0: encoded output x 191 ops/sec ±0.12% (90 runs sampled)
Fastest is gen-mapping:      decoded output

***

babel.min.js.map
gen-mapping:      addSegment x 35.38 ops/sec ±4.48% (48 runs sampled)
gen-mapping:      addMapping x 29.93 ops/sec ±5.03% (55 runs sampled)
source-map-js:    addMapping x 22.19 ops/sec ±3.39% (41 runs sampled)
source-map-0.6.1: addMapping x 22.57 ops/sec ±2.90% (41 runs sampled)
source-map-0.8.0: addMapping x 22.73 ops/sec ±2.84% (41 runs sampled)
Fastest is gen-mapping:      addSegment

gen-mapping:      decoded output x 157,767,591 ops/sec ±0.10% (99 runs sampled)
gen-mapping:      encoded output x 97.06 ops/sec ±1.69% (73 runs sampled)
source-map-js:    encoded output x 17.51 ops/sec ±2.27% (37 runs sampled)
source-map-0.6.1: encoded output x 17.40 ops/sec ±4.61% (35 runs sampled)
source-map-0.8.0: encoded output x 17.83 ops/sec ±4.85% (36 runs sampled)
Fastest is gen-mapping:      decoded output

***

preact.js.map
gen-mapping:      addSegment x 13,516 ops/sec ±0.27% (98 runs sampled)
gen-mapping:      addMapping x 12,117 ops/sec ±0.25% (97 runs sampled)
source-map-js:    addMapping x 4,285 ops/sec ±1.53% (98 runs sampled)
source-map-0.6.1: addMapping x 4,482 ops/sec ±0.20% (100 runs sampled)
source-map-0.8.0: addMapping x 4,482 ops/sec ±0.28% (99 runs sampled)
Fastest is gen-mapping:      addSegment

gen-mapping:      decoded output x 157,769,691 ops/sec ±0.06% (92 runs sampled)
gen-mapping:      encoded output x 18,610 ops/sec ±1.03% (93 runs sampled)
source-map-js:    encoded output x 5,397 ops/sec ±0.38% (97 runs sampled)
source-map-0.6.1: encoded output x 5,422 ops/sec ±0.16% (100 runs sampled)
source-map-0.8.0: encoded output x 5,595 ops/sec ±0.11% (100 runs sampled)
Fastest is gen-mapping:      decoded output

***

react.js.map
gen-mapping:      addSegment x 5,058 ops/sec ±0.27% (100 runs sampled)
gen-mapping:      addMapping x 4,352 ops/sec ±0.13% (97 runs sampled)
source-map-js:    addMapping x 1,569 ops/sec ±0.19% (99 runs sampled)
source-map-0.6.1: addMapping x 1,550 ops/sec ±0.31% (97 runs sampled)
source-map-0.8.0: addMapping x 1,560 ops/sec ±0.18% (99 runs sampled)
Fastest is gen-mapping:      addSegment

gen-mapping:      decoded output x 157,479,701 ops/sec ±0.14% (99 runs sampled)
gen-mapping:      encoded output x 6,392 ops/sec ±1.03% (94 runs sampled)
source-map-js:    encoded output x 2,213 ops/sec ±0.36% (99 runs sampled)
source-map-0.6.1: encoded output x 2,238 ops/sec ±0.23% (100 runs sampled)
source-map-0.8.0: encoded output x 2,304 ops/sec ±0.20% (100 runs sampled)
Fastest is gen-mapping:      decoded output
```

[source-map]: https://www.npmjs.com/package/source-map
[trace-mapping]: https://github.com/jridgewell/trace-mapping
