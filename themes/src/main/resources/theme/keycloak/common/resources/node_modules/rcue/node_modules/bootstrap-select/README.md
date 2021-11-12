<h1 align="center">bootstrap-select</h1>

<p align="center">
	<strong>The jQuery plugin that brings select elements into the 21st century with intuitive multiselection, searching, and much more. Now with Bootstrap 4 support.</strong>
</p>

<p align="center">
	<a href="https://github.com/snapappointments/bootstrap-select/releases/latest" target="_blank">
		<img src="https://img.shields.io/github/release/snapappointments/bootstrap-select.svg" alt="Latest release">
	</a>
	<a href="https://www.npmjs.com/package/bootstrap-select" target="_blank">
		<img src="https://img.shields.io/npm/v/bootstrap-select.svg" alt="npm">
	</a>
	<a href="https://www.nuget.org/packages/bootstrap-select" target="_blank">
		<img src="https://img.shields.io/nuget/v/bootstrap-select.svg" alt="NuGet">
	</a>
	<a href="https://cdnjs.com/libraries/bootstrap-select" target="_blank">
		<img src="https://img.shields.io/cdnjs/v/bootstrap-select.svg" alt="CDNJS">
	</a>
	<br>
	<a href="https://cdnjs.com/libraries/bootstrap-select" target="_blank">
		<img src="https://img.shields.io/badge/license-MIT-brightgreen.svg" alt="License">
	</a>
	<a href="https://david-dm.org/snapappointments/bootstrap-select?type=peer" target="_blank">
		<img src="https://img.shields.io/david/peer/snapappointments/bootstrap-select.svg" alt="peerDependencies Status">
	</a>
	<a href="https://david-dm.org/snapappointments/bootstrap-select#info=devDependencies" target="_blank">
		<img src="https://david-dm.org/snapappointments/bootstrap-select/dev-status.svg" alt="devDependency Status">
	</a>
</p>

<p align="center">
	<a href="https://developer.snapappointments.com/bootstrap-select"><img src="https://user-images.githubusercontent.com/2874325/38997831-97e12bbe-43ab-11e8-85f5-b8c05d91c7b1.gif" width="289" height="396" alt="bootstrap-select demo"></a>
</p>

## Demo and Documentation

You can view a live demo and some examples of how to use the various options [here](https://developer.snapappointments.com/bootstrap-select).

Bootstrap-select's documentation, included in this repo in the root directory, is built with MkDocs and hosted at https://developer.snapappointments.com/bootstrap-select. The documentation may also be run locally.


### Running documentation locally

1. If necessary, [install MkDocs](https://www.mkdocs.org/#installation).
3. From the `/bootstrap-select/docs` directory, run `mkdocs serve` in the command line.
4. Open `http://127.0.0.1:8000/` in your browser, and voilà.

Learn more about using MkDocs by reading its [documentation](https://www.mkdocs.org/).

## Usage

Create your `<select>` with the `.selectpicker` class.
```html
<select class="selectpicker">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Barbecue</option>
</select>
```

If you use a 1.6.3 or newer, you don't need to do anything else, as the data-api automatically picks up the `<select>`s with the `selectpicker` class.

If you use an older version, you need to add the following either at the bottom of the page (after the last selectpicker), or in a [`$(document).ready()`](https://api.jquery.com/ready/) block.
```js
// To style only <select>s with the selectpicker class
$('.selectpicker').selectpicker();
```
Or
```js
// To style all <select>s
$('select').selectpicker();
```

Checkout the [documentation](https://developer.snapappointments.com/bootstrap-select) for further information.

## CDN

**N.B.**: The CDN is updated after the release is made public, which means that there is a delay between the publishing of a release and its availability on the CDN. Check [the GitHub page](https://github.com/snapappointments/bootstrap-select/releases) for the latest release.

* [//cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/css/bootstrap-select.min.css](//cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/css/bootstrap-select.min.css)
* [//cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/js/bootstrap-select.min.js](//cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/js/bootstrap-select.min.js)
* //cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/js/i18n/defaults-*.min.js (The translation files)

## Bugs and feature requests

Anyone and everyone is welcome to contribute. **Please take a moment to
review the [guidelines for contributing](CONTRIBUTING.md)**. Make sure you're using the latest version of bootstrap-select before submitting an issue.

* [Bug reports](CONTRIBUTING.md#bug-reports)
* [Feature requests](CONTRIBUTING.md#feature-requests)

## Copyright and license

Copyright (C) 2012-2018 [SnapAppointments, LLC](https://snapappointments.com)

Licensed under [the MIT license](LICENSE).
