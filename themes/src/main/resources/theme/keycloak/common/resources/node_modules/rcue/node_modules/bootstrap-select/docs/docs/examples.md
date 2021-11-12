# Basic examples

---
## Standard select boxes

<div class="bs-docs-example">
  <p>Make this:</p>

  <select>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>

  <p>Become this:</p>

  <select class="selectpicker">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

<div id="optgroup"></div>
## Select boxes with optgroups

<div class="bs-docs-example">
  <select class="selectpicker">
    <optgroup label="Picnic">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </optgroup>
    <optgroup label="Camping">
      <option>Tent</option>
      <option>Flashlight</option>
      <option>Toilet Paper</option>
    </optgroup>
  </select>
</div>

```html
<select class="selectpicker">
  <optgroup label="Picnic">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </optgroup>
  <optgroup label="Camping">
    <option>Tent</option>
    <option>Flashlight</option>
    <option>Toilet Paper</option>
  </optgroup>
</select>
```

## Multiple select boxes

<div class="bs-docs-example">
  <select class="selectpicker" multiple>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker" multiple>
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

# Live search

---

## Live search

You can add a search input by passing `data-live-search="true"` attribute:

<div class="bs-docs-example no-code">
  <select class="selectpicker" data-live-search="true">
    <option>Hot Dog, Fries and a Soda</option>
    <option>Burger, Shake and a Smile</option>
    <option>Sugar, Spice and all things nice</option>
  </select>
</div>

## Key words

Add key words to options to improve their searchability using `data-tokens`.

<div class="bs-docs-example">
  <select class="selectpicker" data-live-search="true">
    <option data-tokens="ketchup mustard">Hot Dog, Fries and a Soda</option>
    <option data-tokens="mustard">Burger, Shake and a Smile</option>
    <option data-tokens="frosting">Sugar, Spice and all things nice</option>
  </select>
</div>

```html
<select class="selectpicker" data-live-search="true">
  <option data-tokens="ketchup mustard">Hot Dog, Fries and a Soda</option>
  <option data-tokens="mustard">Burger, Shake and a Smile</option>
  <option data-tokens="frosting">Sugar, Spice and all things nice</option>
</select>
```

# Limit the number of selections

Limit the number of options that can be selected via the `data-max-options` attribute. It also works for option groups. Customize the message displayed when the limit is reached with `maxOptionsText`.

<div class="bs-docs-example">
  <select class="selectpicker" multiple data-max-options="2">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>

  <select class="selectpicker" multiple>
    <optgroup label="Condiments" data-max-options="2">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </optgroup>
    <optgroup label="Breads" data-max-options="2">
      <option>Plain</option>
      <option>Steamed</option>
      <option>Toasted</option>
    </optgroup>
  </select>
</div>

```html
<select class="selectpicker" multiple data-max-options="2">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>

<select class="selectpicker" multiple>
  <optgroup label="Condiments" data-max-options="2">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </optgroup>
  <optgroup label="Breads" data-max-options="2">
    <option>Plain</option>
    <option>Steamed</option>
    <option>Toasted</option>
  </optgroup>
</select>
```

# Custom button text

---

## Placeholder
<p id="titleMultiples"></p>
Using the `title` attribute will set the default placeholder text when nothing is selected. This works for both multiple and standard select boxes:

<div class="bs-docs-example">
  <div class="form-group">
    <label>Multiple</label>
    <select class="selectpicker" multiple title="Choose one of the following...">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>

  <div class="form-group">
    <label>Standard</label>
    <select class="selectpicker" title="Choose one of the following...">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
</div>

```html
<select class="selectpicker" multiple title="Choose one of the following...">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

## Selected text

<p id="title"></p>
Set the `title` attribute on individual options to display alternative text when the option is selected:

<div class="bs-docs-example no-code">
  <select class="selectpicker">
    <option title="Combo 1">Hot Dog, Fries and a Soda</option>
    <option title="Combo 2">Burger, Shake and a Smile</option>
    <option title="Combo 3">Sugar, Spice and all things nice</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option title="Combo 1">Hot Dog, Fries and a Soda</option>
  <option title="Combo 2">Burger, Shake and a Smile</option>
  <option title="Combo 3">Sugar, Spice and all things nice</option>
</select>
```
## Selected text format

<p id="titleMultiplesFormat"></p>
Specify how the selection is displayed with the `data-selected-text-format` attribute on a multiple select.

The supported values are:

* `values`: A comma delimited list of selected values (default)
* `count`: If one item is selected, then the option value is shown. If more than one is selected then the number of selected items is displayed, e.g. `2 of 6 selected`
* `count > x`: Where `x` is the number of items selected when the display format changes from `values` to `count`
* `static`: Always show the select title (placeholder), regardless of selection

<div class="bs-docs-example">
  <select class="selectpicker" multiple data-selected-text-format="count">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker" multiple data-selected-text-format="count">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

<div class="bs-docs-example">
  <select class="selectpicker" multiple data-selected-text-format="count > 3">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Onions</option>
  </select>
</div>

```html
<select class="selectpicker" multiple data-selected-text-format="count > 3">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
  <option>Onions</option>
</select>
```

# Styling

---

## Button classes

You can set the button classes via the `data-style` attribute:

<div class="bs-docs-example">
  <div class="form-group">
    <select class="selectpicker" data-style="btn-primary">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
  <div class="form-group">
    <select class="selectpicker" data-style="btn-info">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
  <div class="form-group">
    <select class="selectpicker" data-style="btn-success">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
  <div class="form-group">
    <select class="selectpicker" data-style="btn-warning">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
  <div class="form-group">
    <select class="selectpicker" data-style="btn-danger">
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </select>
  </div>
</div>

```html
<select class="selectpicker" data-style="btn-primary">
  ...
</select>

<select class="selectpicker" data-style="btn-info">
  ...
</select>

<select class="selectpicker" data-style="btn-success">
  ...
</select>

<select class="selectpicker" data-style="btn-warning">
  ...
</select>

<select class="selectpicker" data-style="btn-danger">
  ...
</select>
```

## Checkmark on selected option

You can also show the checkmark icon on standard select boxes with the `show-tick` class:

<div class="bs-docs-example">
  <select class="selectpicker show-tick">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker show-tick">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

## Menu arrow <span class="text-muted small">(deprecated)</span>

The Bootstrap menu arrow can be added with the `show-menu-arrow` class:

<span class="alert alert-danger d-block" role="alert">
  <strong>Note:</strong> This feature has been deprecated and will be removed in v2.0.0.
</span>

<div class="bs-docs-example">
  <select class="selectpicker show-menu-arrow">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker show-menu-arrow">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

## Style individual options

<p id="classes"></p>
Classes and styles added to options are transferred to the select box:

<div class="bs-docs-example">
  <select class="selectpicker">
    <option>Mustard</option>
    <option class="special">Ketchup</option>
    <option style="background: #5cb85c; color: #fff;">Relish</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option>Mustard</option>
  <option class="special">Ketchup</option>
  <option style="background: #5cb85c; color: #fff;">Relish</option>
</select>
```

```css
.special {
  font-weight: bold !important;
  color: #fff !important;
  background: #bc0000 !important;
  text-transform: uppercase;
}
```

## Width

<p id="grid"></p>
Wrap selects in grid columns, or any custom parent element, to easily enforce desired widths.

<div class="bs-docs-example">
  <div class="row">
    <div class="col-sm-3">
      <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
    <div class="col-sm-9">
      <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-4">
       <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
    <div class="col-sm-8">
       <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-5">
      <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
    <div class="col-sm-7">
      <div class="form-group">
        <select class="selectpicker form-control">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
        </select>
      </div>
    </div>
  </div>
</div>

```html
<div class="row">
  <div class="col-sm-3">
    <div class="form-group">
      <select class="selectpicker form-control">
        <option>Mustard</option>
        <option>Ketchup</option>
        <option>Relish</option>
      </select>
    </div>
  </div>
</div>
```

<div id="data-width"></div>

Alternatively, use the `data-width` attribute to set the width of the select. Set `data-width` to `'auto'` to automatically adjust the width of the select to its widest option. `'fit'` automatically adjusts the width of the select to the width of its currently selected option. An exact value can also be specified, e.g., `300px` or `50%`.

<div class="bs-docs-example">
  <div class="row">
    <div class="col-sm-12">
      <div class="form-group">
        <label>width: 'auto'</label>
        <select class="selectpicker form-control" data-width="auto">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
          <option>All of the above (and much, much more!)</option>
        </select>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-12">
      <div class="form-group">
        <label>width: 'fit'</label>
        <select class="selectpicker form-control" data-width="fit">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
          <option>All of the above (and much, much more!)</option>
        </select>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-12">
      <div class="form-group">
        <label>width: '100px'</label>
        <select class="selectpicker form-control" data-width="100px">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
          <option>All of the above (and much, much more!)</option>
        </select>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-12">
      <div class="form-group">
        <label>width: '75%'</label>
        <select class="selectpicker form-control" data-width="75%">
          <option>Mustard</option>
          <option>Ketchup</option>
          <option>Relish</option>
          <option>All of the above (and much, much more!)</option>
        </select>
      </div>
    </div>
  </div>
</div>

```html
<select class="selectpicker" data-width="auto">
  ...
</select>
<select class="selectpicker" data-width="fit">
  ...
</select>
<select class="selectpicker" data-width="100px">
  ...
</select>
<select class="selectpicker" data-width="75%">
  ...
</select>
```

# Customize options

---

## Icons

Add an icon to an option or optgroup with the `data-icon` attribute:

<span class="alert alert-info d-block" role="alert">
  <strong>Note:</strong> Glyphicons are not included in Bootstrap 4. To use FontAwesome, or another icon library, you'll need to set `iconBase` to something other than `'glyphicon'`.
</span>

<div class="bs-docs-example">
  <select class="selectpicker">
    <option data-icon="fa-glass">Mustard</option>
    <option data-icon="fa-heart">Ketchup</option>
    <option data-icon="fa-film">Relish</option>
    <option data-icon="fa-home">Mayonnaise</option>
    <option data-icon="fa-print">Barbecue Sauce</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option data-icon="fa-heart">Ketchup</option>
</select>
```

## Custom content

Insert custom HTML into the option with the `data-content` attribute:

<div class="bs-docs-example">
  <select class="selectpicker">
    <option data-content="<span class='label label-warning'>Mustard</span>">Mustard</option>
    <option data-content="<span class='label label-danger label-important'>Ketchup</span>">Ketchup</option>
    <option data-content="<span class='label label-success'>Relish</span>">Relish</option>
    <option data-content="<span class='label label-info'>Mayonnaise</span>">Mayonnaise</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option data-content="<span class='label label-success'>Relish</span>">Relish</option>
</select>
```

## Subtext
Add subtext to an option or optgroup with the `data-subtext` attribute:

<div class="bs-docs-example">
  <div class="form-group">
    <select class="selectpicker">
      <option data-subtext="French's">Mustard</option>
      <option data-subtext="Heinz">Ketchup</option>
      <option data-subtext="Sweet">Relish</option>
      <option data-subtext="Miracle Whip">Mayonnaise</option>
      <option data-divider="true"></option>
      <option data-subtext="Honey">Barbecue Sauce</option>
      <option data-subtext="Ranch">Salad Dressing</option>
      <option data-subtext="Sweet & Spicy">Tabasco</option>
      <option data-subtext="Chunky">Salsa</option>
    </select>
  </div>

  <div class="form-group">
    <select class="selectpicker" data-show-subtext="true">
      <option data-subtext="French's">Mustard</option>
      <option data-subtext="Heinz">Ketchup</option>
      <option data-subtext="Sweet">Relish</option>
      <option data-subtext="Miracle Whip">Mayonnaise</option>
      <option data-divider="true"></option>
      <option data-subtext="Honey">Barbecue Sauce</option>
      <option data-subtext="Ranch">Salad Dressing</option>
      <option data-subtext="Sweet & Spicy">Tabasco</option>
      <option data-subtext="Chunky">Salsa</option>
    </select>
    <span class="help-block">With <code>showSubtext</code> set to true.</span>
  </div>
</div>

```html
<select class="selectpicker" data-size="5">
  <option data-subtext="Heinz">Ketchup</option>
</select>
```

# Customize menu

---

## Menu size

The `size` option is set to `'auto'` by default. When `size` is set to `'auto'`, the menu always opens up to show as many items as the window will allow without being cut off. Set `size` to `false` to always show all items. The size of the menu can also be specifed using the `data-size` attribute.

<div class="bs-docs-example">
  <select class="selectpicker">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Mayonnaise</option>
    <option>Barbecue Sauce</option>
    <option>Salad Dressing</option>
    <option>Tabasco</option>
    <option>Salsa</option>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Mayonnaise</option>
    <option>Barbecue Sauce</option>
    <option>Salad Dressing</option>
    <option>Tabasco</option>
    <option>Salsa</option>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Mayonnaise</option>
    <option>Barbecue Sauce</option>
    <option>Salad Dressing</option>
    <option>Tabasco</option>
    <option>Salsa</option>
  </select>
</div>

<p id="data-size"></p>
Specify a number for `data-size` to choose the maximum number of items to show in the menu.

<div class="bs-docs-example">
  <select class="selectpicker" data-size="5">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Mayonnaise</option>
    <option>Barbecue Sauce</option>
    <option>Salad Dressing</option>
    <option>Tabasco</option>
    <option>Salsa</option>
  </select>
</div>

```html
<select class="selectpicker" data-size="5">
  ...
</select>
```

## Select/deselect all options

Adds two buttons to the top of the menu - **Select All** & **Deselect All** with `data-actions-box="true"`.

<div class="bs-docs-example">
  <select class="selectpicker" multiple data-actions-box="true">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker" multiple data-actions-box="true">
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

## Divider

Add `data-divider="true"` to an option to turn it into a divider.

<div class="bs-docs-example">
  <select class="selectpicker">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
    <option>Mayonnaise</option>
    <option data-divider="true"></option>
    <option>Barbecue Sauce</option>
    <option>Salad Dressing</option>
    <option>Tabasco</option>
    <option>Salsa</option>
  </select>
</div>

```html
<select class="selectpicker" data-size="5">
  <option data-divider="true"></option>
</select>
```

## Menu header

Add a header to the dropdown menu, e.g. `header: 'Select a condiment'` or `data-header="Select a condiment"`

<div class="bs-docs-example">
  <div class="row">
    <div class="col-md-3">
      <select class="selectpicker form-control" data-header="Select a condiment">
        <option data-subtext="French's">Mustard</option>
        <option data-subtext="Heinz">Ketchup</option>
        <option data-subtext="Sweet">Relish</option>
        <option data-subtext="Miracle Whip">Mayonnaise</option>
        <option data-divider="true"></option>
        <option data-subtext="Honey">Barbecue Sauce</option>
        <option data-subtext="Ranch">Salad Dressing</option>
        <option data-subtext="Sweet & Spicy">Tabasco</option>
        <option data-subtext="Chunky">Salsa</option>
      </select>
    </div>
  </div>
</div>

```html
<select class="selectpicker" data-header="Select a condiment">
  ...
</select>
```

## Container

Append the select to a specific element, e.g. `container: 'body'` or `data-container=".main-content"`

<div class="bs-docs-example" style="overflow:hidden;">
  <div class="row">
    <div class="col-md-3">
      <select class="selectpicker form-control">
        <option data-subtext="French's">Mustard</option>
        <option data-subtext="Heinz">Ketchup</option>
        <option data-subtext="Sweet">Relish</option>
        <option data-subtext="Miracle Whip">Mayonnaise</option>
        <option data-divider="true"></option>
        <option data-subtext="Honey">Barbecue Sauce</option>
        <option data-subtext="Ranch">Salad Dressing</option>
        <option data-subtext="Sweet & Spicy">Tabasco</option>
        <option data-subtext="Chunky">Salsa</option>
      </select>
    </div>
    <div class="col-md-3">
      <select class="selectpicker form-control" data-container="body">
        <option data-subtext="French's">Mustard</option>
        <option data-subtext="Heinz">Ketchup</option>
        <option data-subtext="Sweet">Relish</option>
        <option data-subtext="Miracle Whip">Mayonnaise</option>
        <option data-divider="true"></option>
        <option data-subtext="Honey">Barbecue Sauce</option>
        <option data-subtext="Ranch">Salad Dressing</option>
        <option data-subtext="Sweet & Spicy">Tabasco</option>
        <option data-subtext="Chunky">Salsa</option>
      </select>
    </div>
  </div>
</div>

```html
<div style="overflow:hidden;">
  <select class="selectpicker">
    ...
  </select>
  <select class="selectpicker" data-container="body">
    ...
  </select>
</div>
```

## Dropup menu

`dropupAuto` is set to true by default, which automatically determines whether or not the menu should display above or below the select box. If `dropupAuto` is set to false, manually make the select a dropup menu by adding the `.dropup` class to the select.

<div class="bs-docs-example">
  <select class="selectpicker dropup">
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker dropup">
  ...
</select>
```

# Disabled

---

## Disabled select box

<div class="bs-docs-example">
  <select class="selectpicker" disabled>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker" disabled>
  <option>Mustard</option>
  <option>Ketchup</option>
  <option>Relish</option>
</select>
```

## Disabled options

<div class="bs-docs-example">
  <select class="selectpicker">
    <option>Mustard</option>
    <option disabled>Ketchup</option>
    <option>Relish</option>
  </select>
</div>

```html
<select class="selectpicker">
  <option>Mustard</option>
  <option disabled>Ketchup</option>
  <option>Relish</option>
</select>
```

## Disabled option groups

<div class="bs-docs-example">
  <select class="selectpicker test">
    <optgroup label="Picnic" disabled>
      <option>Mustard</option>
      <option>Ketchup</option>
      <option>Relish</option>
    </optgroup>
    <optgroup label="Camping">
      <option>Tent</option>
      <option>Flashlight</option>
      <option>Toilet Paper</option>
    </optgroup>
  </select>
</div>

```html
<select class="selectpicker test">
  <optgroup label="Picnic" disabled>
    <option>Mustard</option>
    <option>Ketchup</option>
    <option>Relish</option>
  </optgroup>
  <optgroup label="Camping">
    <option>Tent</option>
    <option>Flashlight</option>
    <option>Toilet Paper</option>
  </optgroup>
</select>
```