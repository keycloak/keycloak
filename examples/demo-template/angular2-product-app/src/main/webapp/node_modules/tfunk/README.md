##tfunk [![Build Status](https://travis-ci.org/shakyShane/tfunk.svg)](https://travis-ci.org/shakyShane/tfunk)

Multi-colour console output from [Chalk](https://github.com/sindresorhus/chalk#styles) with added awesome.

by [@shakyshane](https://github.com/shakyShane) & [@AydinHassan](https://github.com/AydinHassan)

![tfunk](http://f.cl.ly/items/15102k441h1U1Z1l253J/Screen%20Shot%202014-09-10%20at%2022.05.15.png)

##Install

```bash
npm install tfunk
```

##Usage

**Syntax rules:**

`{` `<color>` `:` `YOUR STRING` `}`

**Example**

`{blue:This is a blue line}`

**`}` is optional**

`{blue:This is a blue line` <- Perfectly valid 


##Usage
```js
var tFunk = require("tfunk");

console.log( tfunk("{cyan:tFunk terminal colours") )

// => tFunk terminal colours
```

Or get a custom compiler with a set prefix:

```js
var compiler = require("tfunk").Compiler({
    prefix: "[{magenta:tFunk}]"
});

console.log( compiler.compile("tFunk is awesome") );
console.log( compiler.compile("don't you think?") );

// => [tFunk] tFunk is awesome
// => [tFunk] don't you think?
```

**Define your own syntax**

You can define your own methods, they receive the string section as the first parameter & have access to the compiler
through `this.compile()` keyword.

```js
var compiler = require("tfunk").Compiler({
    "warn": function(string) {
        return this.compile("{red:WARNING:" + string);
    }
});
```

Now you can use `warn` anywhere you like.

```js
console.log( compiler.compile("{warn: Could not file your config file...") );

// => WARNING: Could not file your config file...
```

##Examples

Here are some comparisons to chalk, to help you understand how to use tFunk.

###Single Colours

```js
// chalk
console.log( chalk.red("This has a single colour") );

// tFunk
console.log( tFunk("{red:This has a single colour") );
```

###Single Colour mid string

```js
// chalk
console.log( "This has a single colour " + chalk.cyan("that begins mid-string") );

// tFunck
console.log( tFunk("This has a single colour {cyan:that begins mid-string") );
```

###Single Colour with end point

```js
// chalk
console.log( chalk.red("This has a single colour with ") + "an endpoint");

// tFunk
console.log( tFunk("{red:This has a single colour with }an endpoint") );
```

###Two Colours

```js
// chalk
console.log( chalk.green("This has ") + chalk.cyan("two colours") );

// tFunk
console.log( tFunk("{green:This has {cyan:two colours") );
```

###Nested Colours

```js
// chalk
console.log( chalk.green("This has a colour " + chalk.cyan("nested inside") + " another colour") );

//tFunk
console.log( tFunk("{green:This has a colour {cyan:nested inside} another colour") );
```

###Multiple Nested

```js
// chalk
console.log( chalk.blue("Multiple " + chalk.cyan("NESTED") + " styles in " + chalk.red("the same string") + " with an ending") );

// tFunk
console.log( tFunk("{blue:Multiple {cyan:NESTED} styles in {red:the same string} with an ending") );
```

###Multi line
```js
var multiline = require("multiline");

var string = multiline(function () {/*
{cyan:This is a multi-line coloured string
With a single {yellow:yellow} word in the center of a line
Pretty cool huh?
*/});

console.log( tFunk(string) );
```

###Escaping when you need curly braces
```js
console.log( tFunk("This has a \\{\\{mustache\\}\\}") );
```


##TODO
- [x] Colours
- [x] Nested Colours
- [x] Custom syntax
- [x] Prefixed compiler
- [x] Make the chain-able API work like this `"{white.bgRed: White text, red BG"`
- [x] Offer a way of escaping. Right now, ALL instances of `}` will be lost
