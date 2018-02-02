## Filing an issue

Before filing an issue, please [search the queue](https://github.com/c3js/c3/issues) to make sure it hasn't already been reported.

If a bug, please include the following —

1. What version of C3?
1. What browsers have you confirmed it in?
1. Can you isolate the issue by providing a jsFiddle demonstrating it in a minimalist capacity?

Please *do not* ask for support using the issue queue. For support, please ask [on chat](https://gitter.im/c3js/c3) or [the mailing list](groups.google.com/forum/#!forum/c3js).

## Setup
 1. **Clone the repo from GitHub**

        git clone https://github.com/c3js/c3.git
        cd c3

 2. **Acquire build dependencies.** Make sure you have [Node.js](http://nodejs.org/) installed on your workstation. This is only needed to _build_ C3 from sources. C3 itself has no dependency on Node.js once it is built. Now run:

        npm install -g grunt-cli
        npm install

    The first `npm` command sets up the popular [Grunt](http://gruntjs.com/) build tool. You might need to run this command with `sudo` if you're on Linux or Mac OS X, or in an Administrator command prompt on Windows. The second `npm` command fetches the remaining build dependencies.

## Building C3 from sources
    npm run build


## Distribution
    npm run dist

Now you'll find the built files in `c3.js`, `c3.min.js`, `c3.css` & `c3.min.css`.

## Running the tests
    npm run test

This command will automatically run the specification suite and report its results.

If you want to see specs running live in browser (e.g., for debugging), simply open `http://localhost:9876/` in your browser when phantomjs starts.

## Contributing your changes

Add something about PRs here, indicate that PRs should not bump the version number & the build output files (`c3.js`, `c3.min.js`, `c3.css` & `c3.min.css`) should be excluded
