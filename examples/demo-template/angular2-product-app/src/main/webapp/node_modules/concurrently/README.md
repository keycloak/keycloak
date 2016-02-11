# Concurrently

[![Build Status](https://travis-ci.org/kimmobrunfeldt/concurrently.svg)](https://travis-ci.org/kimmobrunfeldt/concurrently)

**Version: 1.0.0** ([*previous stable*](https://github.com/kimmobrunfeldt/concurrently/tree/0.1.1))

Run multiple commands concurrently.
Like `npm run watch-js & npm run watch-less` but better.

![](docs/demo.gif)

It also works on Windows. You can tune your npm scripts to work across platforms.

## Install

The tool is written in Node.js, but you can use it to run **any** commands.

```bash
npm install -g concurrently
```

## Usage

Remember to surround separate commands with quotes, like this:
```bash
concurrent "command1 arg" "command2 arg"
```

Otherwise **concurrent** would try to run 4 separate commands:
`command1`, `arg`, `command2`, `arg`.

Help:

```
  Usage: concurrent [options] <command ...>

  Options:

    -h, --help                    output usage information
    -V, --version                 output the version number
    -k, --kill-others             kill other processes if one exits or dies
    --no-color                    disable colors from logging
    -p, --prefix <prefix>         prefix used in logging for each process.
    Possible values: index, pid, command, none. Default: index

    -r, --raw                     output only raw output of processes, disables prettifying and colors
    -l, --prefix-length <length>  limit how many characters of the command is displayed in prefix.
    The option can be used to shorten long commands.
    Works only if prefix is set to "command". Default: 10


  Examples:

   - Kill other processes if one exits or dies

       $ concurrent --kill-others "grunt watch" "http-server"

   - Output nothing more than stdout+stderr of child processes

       $ concurrent --raw "npm run watch-less" "npm run watch-js"

   - Normal output but without colors e.g. when logging to file

       $ concurrent --no-color "grunt watch" "http-server" > log

  For more details, visit https://github.com/kimmobrunfeldt/concurrently
```

## FAQ

* Process exited with code *null*?

    From [Node child_process documentation](http://nodejs.org/api/child_process.html#child_process_event_exit), `exit` event:

    > This event is emitted after the child process ends. If the process
    > terminated normally, code is the final exit code of the process,
    > otherwise null. If the process terminated due to receipt of a signal,
    > signal is the string name of the signal, otherwise null.


    So *null* means the process didn't terminate normally. This will make **concurrent**
    to return non-zero exit code too.


## Why

I like [task automation with npm](http://substack.net/task_automation_with_npm_run)
but the usual way to run multiple commands concurrently is
```npm run watch-js & npm run watch-css```. That's fine but it's hard to keep
on track of different outputs. Also if one process fails, others still keep running
and you won't even notice the difference.

Another option would be to just run all commands in separate terminals. I got
tired of opening terminals and made **concurrently**.

### NPM Issue

Previously I thought this could fix some problems I had with watching scripts and this readme said:

> When running watch or serve tasks, I'd recommend to use `--kill-others` option:
>
> ```bash
> concurrent --kill-others "npm run watch-js" "npm run watch-less"
> ```
>
> That way, if for some reason e.g. your `watch-less` died, you would notice it easier.

However NPM didn't work as I hoped it would. See [this issue](https://github.com/kimmobrunfeldt/concurrently/issues/4).
