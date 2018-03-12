ui-select2   [![Build Status](https://travis-ci.org/angular-ui/ui-select2.png)](https://travis-ci.org/angular-ui/ui-select2)
==========
This directive allows you to enhance your select elements with behaviour from the [select2](http://ivaynberg.github.io/select2/) library.

# Requirements

- [AngularJS](http://angularjs.org/)
- [JQuery](http://jquery.com/)
- [Select2](http://ivaynberg.github.io/select2/)

## Setup

1. Install **Karma**, **Grunt** and **Bower**
  `$ npm install -g karma grunt-cli bower`
2. Install development dependencies
  `$ npm install`
3. Install components
  `$ bower install`
4. ???
5. Profit!

## Testing

We use [Grunt](http://gruntjs.com/) to check for JavaScript syntax errors and execute all unit tests. To run Grunt, simply execute:

`$ grunt`

This will lint and test the code, then exit. To have Grunt stay open and automatically lint and test your files whenever you make a code change, use:

`$ grunt karma:server watch`

This will start a Karma server in the background and run unit tests in Firefox and PhantomJS whenever the source code or spec file is saved.

# Usage

We use [bower](https://github.com/bower/bower) for dependency management. Install AngularUI Select2 into your project by running the command

`$ bower install angular-ui-select2`

If you use a `bower.json` file in your project, you can have Bower save ui-select2 as a dependency by passing the `--save` or `--save-dev` flag with the above command.

This will copy the ui-select2 files into your `bower_components` folder, along with its dependencies. Load the script files in your application:
```html
<link rel="stylesheet" href="bower_components/select2/select2.css">
<script type="text/javascript" src="bower_components/jquery/jquery.js"></script>
<script type="text/javascript" src="bower_components/select2/select2.js"></script>
<script type="text/javascript" src="bower_components/angular/angular.js"></script>
<script type="text/javascript" src="bower_components/angular-ui-select2/src/select2.js"></script>
```

(Note that `jquery` must be loaded before `angular` so that it doesn't use `jqLite` internally)


Add the select2 module as a dependency to your application module:

```javascript
var myAppModule = angular.module('MyApp', ['ui.select2']);
```

Apply the directive to your form elements:

```html
<select ui-select2 ng-model="select2" data-placeholder="Pick a number">
    <option value=""></option>
    <option value="one">First</option>
    <option value="two">Second</option>
    <option value="three">Third</option>
</select>
```

## Options

All the select2 options can be passed through the directive. You can read more about the supported list of options and what they do on the [Select2 Documentation Page](http://ivaynberg.github.com/select2/)

```javascript
myAppModule.controller('MyController', function($scope) {
    $scope.select2Options = {
        allowClear:true
    };
});
```

```html
<select ui-select2="select2Options" ng-model="select2">
    <option value="one">First</option>
    <option value="two">Second</option>
    <option value="three">Third</option>
</select>
```

Some times it may make sense to specify the options in the template file.

```html
<select ui-select2="{ allowClear: true}" ng-model="select2">
    <option value="one">First</option>
    <option value="two">Second</option>
    <option value="three">Third</option>
</select>
```

To define global defaults, you can configure the `uiSelect2Config` injectable:

```javascript
myAppModule.run(['uiSelect2Config', function(uiSelect2Config) {
	uiSelect2Config.placeholder = "Placeholder text";
}]);
```

## Working with ng-model

The ui-select2 directive plays nicely with ng-model and validation directives such as ng-required.

If you add the ng-model directive to same the element as ui-select2 then the picked option is automatically synchronized with the model value.

## Working with dynamic options
`ui-select2` is incompatible with `<select ng-options>`. For the best results use `<option ng-repeat>` instead.
```html
<select ui-select2 ng-model="select2" data-placeholder="Pick a number">
    <option value=""></option>
    <option ng-repeat="number in range" value="{{number.value}}">{{number.text}}</option>
</select>
```

## Working with placeholder text
In order to properly support the Select2 placeholder, create an empty `<option>` tag at the top of the `<select>` and either set a `data-placeholder` on the select element or pass a `placeholder` option to Select2.
```html
<select ui-select2 ng-model="number" data-placeholder="Pick a number">
    <option value=""></option>
    <option value="one">First</option>
    <option value="two">Second</option>
    <option value="three">Third</option>
</select>
```

## ng-required directive

If you apply the required directive to element then the form element is invalid until an option is selected.

Note: Remember that the ng-required directive must be explicitly set, i.e. to "true".  This is especially true on divs:

```html
<select ui-select2 ng-model="number" data-placeholder="Pick a number" ng-required="true">
    <option value=""></option>
    <option value="one">First</option>
    <option value="two">Second</option>
    <option value="three">Third</option>
</select>
```

## Using simple tagging mode

When AngularJS View-Model tags are stored as a list of strings, setting
the ui-select2 specific option `simple_tags` will allow to keep the model
as a list of strings, and not convert it into a list of Select2 tag objects.

```html
<input
    type="hidden"
    ui-select2="select2Options"
    ng-model="list_of_string"
    >
```

```javascript
myAppModule.controller('MyController', function($scope) {
    $scope.list_of_string = ['tag1', 'tag2']
    $scope.select2Options = {
        'multiple': true,
        'simple_tags': true,
        'tags': ['tag1', 'tag2', 'tag3', 'tag4']  // Can be empty list.
    };
});
```
