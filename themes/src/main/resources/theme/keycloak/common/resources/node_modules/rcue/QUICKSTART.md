# Using Red Hat Common User Experience (RCUE) - Guide to move from PatternFly to RCUE

RCUE is based on [PatternFly](https://www.patternfly.org/), which is based on [Bootstrap v3](http://getbootstrap.com/).  Think of RCUE as a "skinned" version of PatternFly. **RCUE is meant to be used as a replacement for PatternFly**, so please don't include the PatternFly (or Bootstrap) CSS if you are including the RCUE CSS in your project.

This guide will walk the steps to go from a PatternFly to RCUE styles:

## 1. Installation

### Get RCUE

RCUE can be installed and managed through [NPM](https://www.npmjs.com/). To do so, either add `rcue` as a dependency in your `package.json` or run the following:

```
npm install rcue --save
```

### What's Included

Within the `node_module/rcue/dist` folder you'll find the following directories and files, logically grouping common assets and providing both compiled and minified variations. You'll see something like this:

```
rcue/
├── dist/
│   ├── css/
│   │   │── rcue.min.css (* need to include)
│   │   │── rcue-additions.min.css (* need to include)
│   └── img/
│   │   │── branding materials and loading indicators
├── less/
│   ├── variables, mixin, and component Less files (may need to include if you want to customize the already built CSS)
└── tests/
    ├── example markup source files
```

RCUE provides compiled CSS (`rcue.*`), as well as compiled and minified CSS (`rcue.min.*`). CSS [source maps](https://developer.chrome.com/devtools/docs/css-preprocessors) (`rcue.*.map`) are available for use with certain browsers' developer tools.

## 2. Replace PatternFly with RCUE
Look for the PatternFly folder on your project and replace it with the RCUE folder you've just downloaded.

This swap will break your project paths, but don't worry! it's an easy fix we will discuss later on.

## 3. Using RCUE In Your Application

Your last task is to change the paths of your project and point them to the new RCUE folder.

1. Add the following CSS includes to your HTML file(s), adjusting path where needed:

        <!-- RCUE Styles -->
        <!-- Note: No other CSS files are needed regardless of what other JS packages you decide to pull in -->
        <link rel="stylesheet" href="PATH-TO/node_modules/rcue/dist/css/rcue.min.css" />
        <link rel="stylesheet" href="PATH-TO/node_modules/rcue/dist/css/rcue-additions.min.css" />

2. Add the following script includes to your HTML file(s), adjusting where necessary to pull in only what you need:

        <!-- jQuery -->
        <script src="PATH-TO/node_modules/rcue/node_modules/jquery/dist/jquery.js"></script>

        <!-- Bootstrap JS -->
        <script src="PATH-TO/node_modules/rcue/node_modules/bootstrap/dist/js/bootstrap.js"></script>

        <!-- C3, D3 - Charting Libraries -->
        <script src="PATH-TO/node_modules/rcue/node_modules/c3/c3.min.js"></script>
        <script src="PATH-TO/node_modules/rcue/node_modules/d3/d3.min.js"></script>

        <!-- Datatables, jQuery Grid Component -->
        <!-- Note: jquery.dataTables.js must occur in the html source before patternfly*.js.-->
        <script src="PATH-TO/node_modules/rcue/node_modules/datatables/media/js/jquery.dataTables.js"></script>
        <script src="PATH-TO/node_modules/rcue/node_modules/drmonty-datatables-colvis/js/dataTables.colVis.js"></script>
        <script src="PATH-TO/node_modules/rcue/node_modules/datatables.net-colreorder/js/dataTables.colReorder.js"></script>

        <!-- Patternfly Custom Componets -  Sidebar, Popovers and Datatables Customizations -->
        <!-- Note: jquery.dataTables.js must occur in the html source before patternfly*.js.-->
        <script src="PATH-TO/node_modules/rcue/dist/js/patternfly.js"></script>

        <!-- Bootstrap Date Picker -->
        <script src="PATH-TO/node_modules/rcue/node_modules/bootstrap-datepicker/dist/js/bootstrap-datepicker.min.js"></script>

        <!-- Bootstrap Combobox -->
        <script src="PATH-TO/node_modules/rcue/node_modules/patternfly-bootstrap-combobox/js/bootstrap-combobox.js"></script>

        <!-- Bootstrap Select -->
        <script src="PATH-TO/node_modules/rcue/node_modules/bootstrap-select/dist/js/bootstrap-select.min.js"></script>

        <!-- Bootstrap Tree View -->
        <script src="PATH-TO/node_modules/rcue/node_modules/patternfly-bootstrap-treeview/dist/bootstrap-treeview.min.js"></script>

        <!-- Google Code Prettify - Syntax highlighting of code snippets -->
        <script src="PATH-TO/node_modules/rcue/node_modules/google-code-prettify/bin/prettify.min.js"></script>

        <!-- MatchHeight - Used to make sure dashboard cards are the same height -->
        <script src="PATH-TO/node_modules/rcue/node_modules/jquery-match-height/dist/jquery.matchHeight.js"></script>

        <!-- Angular Application? You May Want to Consider Pulling Angular-Patternfly And Angular-UI Bootstrap instead of bootstrap.js -->
        <!-- See https://github.com/patternfly/angular-patternfly for more information -->

## 6. Enjoy

You are done :smile:

If you have any question please contact [the UXD team](mailto:uxd-team@redhat.com).
