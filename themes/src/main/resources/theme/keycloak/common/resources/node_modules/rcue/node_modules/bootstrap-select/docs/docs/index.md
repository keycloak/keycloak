# Getting Started

---

## Dependencies

Requires jQuery v1.8.0+, Bootstrap’s dropdown.js component, and Bootstrap's CSS. If you're not already using Bootstrap in your project, a precompiled version of the minimum requirements can be downloaded [here](https://getbootstrap.com/docs/3.3/customize/?id=7830063837006f6fc84f).

## CDNJS

The folks at CDNJS host a copy of the library. The CDN is updated after the release is made public, which means there is a delay between the publishing of a release and its availability on the CDN, so keep that in mind. Just use these links:

```html
<!-- Latest compiled and minified CSS -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/css/bootstrap-select.min.css">

<!-- Latest compiled and minified JavaScript -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/js/bootstrap-select.min.js"></script>

<!-- (Optional) Latest compiled and minified JavaScript translation files -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.1/js/i18n/defaults-*.min.js"></script>
```

## Install with Bower

You can also install bootstrap-select using [Bower](http://bower.io):

```elixir
$ bower install bootstrap-select
```

## Install with npm

You can also install bootstrap-select using [npm](https://www.npmjs.com/package/bootstrap-select):

```elixir
$ npm install bootstrap-select
```

## Install with NuGet

You can also install bootstrap-select using [NuGet](https://www.nuget.org/packages/bootstrap-select):

```elixir
$ Install-Package bootstrap-select
```

# Usage

---

Create your `<select>` with the `.selectpicker` class. The data-api will automatically theme these elements.

```html
<select class="selectpicker">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

Options can be passed via data attributes or JavaScript.

```js
$('.selectpicker').selectpicker({
  style: 'btn-info',
  size: 4
});
```


