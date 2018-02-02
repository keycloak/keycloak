# Bootstrap Switch
[![Dependency Status](https://david-dm.org/Bttstrp/bootstrap-switch.svg?style=flat)](https://david-dm.org/Bttstrp/bootstrap-switch)
[![devDependency Status](https://david-dm.org/Bttstrp/bootstrap-switch/dev-status.svg?style=flat)](https://david-dm.org/Bttstrp/bootstrap-switch#info=devDependencies)
[![NPM Version](http://img.shields.io/npm/v/bootstrap-switch.svg?style=flat)](https://www.npmjs.org/)

Turn checkboxes and radio buttons into toggle switches. Created by [Mattia Larentis](http://github.com/nostalgiaz), maintained by [Emanuele Marchi](http://github.com/lostcrew) and [Peter Stein](http://www.bdmdesign.org) with the help of the community.

To get started, check out [https://bttstrp.github.io/bootstrap-switch](https://bttstrp.github.io/bootstrap-switch)!

## Quick start

Several quick start options are available:

- Download the [latest release](https://github.com/Bttstrp/bootstrap-switch/releases/latest)
- Clone the repo: `git clone https://github.com/Bttstrp/bootstrap-switch.git`
- Install with npm: `npm install bootstrap-switch`
- Install with yarn: `yarn add bootstrap-switch`
- Install with Composer: `composer require components/bootstrap-switch`
- Install with Bower: `bower install bootstrap-switch`
- Install with NuGet: `PM> Install-Package Bootstrap.Switch` ([NuGet package](https://github.com/blachniet/bootstrap-switch-nuget))

Include the dependencies: jQuery, Bootstrap and Bootstrap Switch CSS + Javascript:

``` html
<link href="bootstrap.css" rel="stylesheet">
<link href="bootstrap-switch.css" rel="stylesheet">
<script src="jquery.js"></script>
<script src="bootstrap-switch.js"></script>
```

Add your checkbox:

```html
<input type="checkbox" name="my-checkbox" checked>
```

Initialize Bootstrap Switch on it:

```javascript
$("[name='my-checkbox']").bootstrapSwitch();
```

Enjoy.

## Supported browsers

IE9+ and all the other modern browsers.

## LESS

- For Bootstrap 2 (no longer officially supported), import `src/less/bootstrap2/bootstrap-switch.less`
- For Bootstrap 3, import `src/less/bootstrap3/bootstrap-switch.less`

## Bugs and feature requests

Have a bug or a feature request? Please first search for existing and closed issues. If your problem or idea is not addressed yet, [please open a new issue](https://github.com/Bttstrp/bootstrap-switch/issues/new).

The new issue should contain both a summary of the issue and the browser/OS environment in which it occurs and a link to the playground you prefer with the reduced test case.
If suitable, include the steps required to reproduce the bug.

Please do not use the issue tracker for personal support requests: [Stack Overflow](https://stackoverflow.com/questions/tagged/bootstrap-switch) is a better place to get help.

### Known issues

- Make sure `.form-control` is not applied to the input. Bootstrap does not support that, refer to [Checkboxes and radios](https://getbootstrap.com/css/#checkboxes-and-radios)

## Integrations

- Angular: [angular-bootstrap-switch](https://github.com/frapontillo/angular-bootstrap-switch)
- Angular: [angular-toggle-switch](https://github.com/JumpLink/angular-toggle-switch)
- Knockout: [knockout-bootstrap-switch](https://github.com/pauloortins/knockout-bootstrap-switch)

## License

Licensed under the [MIT License](https://github.com/Bttstrp/bootstrap-switch/blob/master/LICENSE).

