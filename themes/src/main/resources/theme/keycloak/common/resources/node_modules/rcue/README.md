# [Red Hat Common User Experience (RCUE)](https://redhat-rcue.github.io/) reference implementation

The [Red Hat Common User Experience (RCUE)](https://redhat-rcue.github.io/) project was created to promote design commonality across all of Red Hat’s Enterprise product offerings.

This reference implementation of RCUE is based on [PatternFly](https://www.patternfly.org/) and [Bootstrap v3](http://getbootstrap.com/).  Think of RCUE as a "skinned" version of Bootstrap with additional components and customizations.  For information on how to quickly get started using RCUE, see the [Quick Start Guide](QUICKSTART.md).

## Dependencies

RCUE includes a number of dependencies that are not committed to this repository.  To add them, see "Install NPM Dependencies".  And make sure you keep them updated (see "Keeping NPM Dependencies Updated").

## Development

Development setup requires nodejs and Ruby. If you do not already have nodejs, npm, and Ruby installed on your system, see https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager and https://www.ruby-lang.org/en/downloads.

### Install NPM Dependencies

The development includes the use of a number of helpful tasks. In order to setup your development environment to allow running of these tasks, you need to install the local nodejs packages declared in `package.json`. To do this run:

    npm install

This will install all necessary development packages into `node_modules/`. At this point, the gruntjs tasks are available for use such as starting a local development server or building the master CSS file.

Additionally you may need to install the grunt command line utility.  To do this run:

    npm install -g grunt-cli

Test pages are generated using [Jekyll](http://jekyllrb.com/).  After ensuring Ruby is installed and available, run:

    gem install jekyll

#### Keeping NPM Dependencies Updated

Anytime you pull a new version of RCUE, make sure you also run

    npm update

so you get the latest version of the components specified in package.json.

### Live Reload Server

A local development server can be quickly fired up by using the Gruntjs server task:

    grunt server

This local static asset server (i.e., [http://localhost:9001](http://localhost:9001)) has the advantage of having livereload integration. Thus, if you start the Gruntjs server, any changes you make to `.html` or `.less` files will be automatically reloaded into your browser and the changes reflected almost immediately. This has the obvious benefit of not having to refresh your browser and still be able to see the changes as you add or remove them from your development files.

### Coding Style

* Indentation
    * Use spaces (not tabs)
    * Indentation size is 2 spaces
* Filenames
    * All filenames will use a lowercase-hyphenated naming convention (e.g., single-select-dropdown.less)
* LESSCSS
    * CSS class names use lowercase-hyphenated naming convention (e.g., .navbar-nav)
    * Alphabetize rules by selector
    * Alphabetize properties by declaration
    * Define or override variables centrally in less/variables.less
    * Define or override mixins centrally in less/mixins.less

## Build

### CSS

In development, styling is written and managed through multiple lesscss files. In order to generate a CSS file of all styling, run the build Gruntjs task:

    grunt build

This task will compile and minify the lesscss files into a single CSS file located at `dist/css/rcue.min.css`.

## Tests

The `tests/` directory contains HTML pages with component and pattern examples in order to facilitate development.  Please consult the official documentation (see below) for full details on how to use RCUE.

The HTML pages in `tests/` are generated using Jekyll.  Do *not* edit these files directly.  Changes to the test source files (`components/patternfly/tests-src/`) should be made upstream in PatternFly.

## Release

To release a new version version of RCUE, edit `bower.json` and `package.json`.

Update the version listed in `bower.json` by editing the file and changing the line:

```
"version": "<new_version>"
```

Update the version listed in `package.json` by editing the file and changing the line:

```
"version": "<new_version>"
```

Commit the version bump:

```
git commit -a -m "Version bump to <new_version>"
```

Tag and push upstream (assuming you have commit access):

```
git tag <new_version>
git push && git push --tags
```

## Documentation

See [https://redhat-rcue.github.io/](https://redhat-rcue.github.io/), [https://www.patternfly.org/](https://www.patternfly.org/), and [http://getbootstrap.com/](http://getbootstrap.com/).

### Browser and Device Support

Since RCUE is based on PatternFly, and PatternFly is based on Bootstrap, RCUE supports [the same browsers as Bootstrap](http://getbootstrap.com/getting-started/#support) **excluding Internet Explorer 8**, plus the latest version of [Firefox for Linux](https://support.mozilla.org/en-US/kb/install-firefox-linux).

*Important:*  starting with the v2.0.0 release, **RCUE no longer supports Internet Explorer 8**.

## License

Modifications to Bootstrap are copyright 2013 Red Hat, Inc. and licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
