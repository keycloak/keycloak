# Methods

Interface with bootstrap-select.

---

#### `.selectpicker('val')`

You can set the selected value by calling the `val` method on the element.

```js
$('.selectpicker').selectpicker('val', 'Mustard');
$('.selectpicker').selectpicker('val', ['Mustard','Relish']);
```

This is different to calling `val()` directly on the `select` element. If you call `val()` on the element directly, the bootstrap-select ui will not refresh (as the change event only fires from user interaction). You will have to call the ui refresh method yourself.

```js
$('.selectpicker').val('Mustard');
$('.selectpicker').selectpicker('render');

// this is the equivalent of the above
$('.selectpicker').selectpicker('val', 'Mustard');
```

---

#### `.selectpicker('selectAll')`

This will select all items in a multi-select.

```js
$('.selectpicker').selectpicker('selectAll');
```

---

#### `.selectpicker('deselectAll')`

This will deselect all items in a multi-select.

```js
$('.selectpicker').selectpicker('deselectAll');
```

---

#### `.selectpicker('render')`

You can force a re-render of the bootstrap-select ui with the `render` method. This is useful if you programatically change any underlying values that affect the layout of the element.

```js
$('.selectpicker').selectpicker('render');
```

---

#### `.selectpicker('mobile')`

Enable mobile scrolling by calling `$('.selectpicker').selectpicker('mobile')`. This enables the device's native menu for select menus.

The method for detecting the browser is left up to the user.

```js
if( /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ) {
  $('.selectpicker').selectpicker('mobile');
}
```

---

#### `.selectpicker('setStyle')`

Modify the class(es) associated with either the button itself or its container.

If changing the class on the container:

```js
$('.selectpicker').addClass('col-lg-12').selectpicker('setStyle');
```

If changing the class(es) on the button (altering data-style):

```js
// Replace Class
$('.selectpicker').selectpicker('setStyle', 'btn-danger');

// Add Class
$('.selectpicker').selectpicker('setStyle', 'btn-large', 'add');

// Remove Class
$('.selectpicker').selectpicker('setStyle', 'btn-large', 'remove');
```


---

#### `.selectpicker('refresh')`

To programmatically update a select with JavaScript, first manipulate the select, then use the `refresh` method to 
update the UI to match the new state. This is necessary when removing or adding options, or when disabling/enabling a 
select via JavaScript.

```js
$('.selectpicker').selectpicker('refresh');
```

<div class="bs-docs-example">
  <div class="form-group">
    <select class="selectpicker remove-example">
      <option value="Mustard">Mustard</option>
      <option value="Ketchup">Ketchup</option>
      <option value="Relish">Relish</option>
    </select>
  </div>

  <button class="btn btn-warning rm-mustard">Remove Mustard</button>
  <button class="btn btn-danger rm-ketchup">Remove Ketchup</button>
  <button class="btn btn-success rm-relish">Remove Relish</button>
</div>

```html
<select class="selectpicker remove-example">
  <option value="Mustard">Mustard</option>
  <option value="Ketchup">Ketchup</option>
  <option value="Relish">Relish</option>
</select>

<button class="btn btn-warning rm-mustard">Remove Mustard</button>
<button class="btn btn-danger rm-ketchup">Remove Ketchup</button>
<button class="btn btn-success rm-relish">Remove Relish</button>
```
```js
$('.rm-mustard').click(function () {
  $('.remove-example').find('[value=Mustard]').remove();
  $('.remove-example').selectpicker('refresh');
});
```

<div class="bs-docs-example">
  <div class="form-group">
    <select class="selectpicker disable-example">
      <option value="Mustard">Mustard</option>
      <option value="Ketchup">Ketchup</option>
      <option value="Relish">Relish</option>
    </select>
  </div>

  <button class="btn btn-default ex-disable"><i class="icon-remove"></i> Disable</button>
  <button class="btn btn-default ex-enable"><i class="icon-ok"></i> Enable</button>
</div>

```js
$('.ex-disable').click(function () {
  $('.disable-example').prop('disabled', true);
  $('.disable-example').selectpicker('refresh');
});

$('.ex-enable').click(function () {
  $('.disable-example').prop('disabled', false);
  $('.disable-example').selectpicker('refresh');
});
```

<script type="text/javascript">
  window.onload = function () {
    var $re = $('.remove-example'),
        $de = $('.disable-example');

    $('.rm-mustard').click(function () {
      $re.find('[value=Mustard]').remove();
      $re.selectpicker('refresh');
    });
    $('.rm-ketchup').click(function () {
      $re.find('[value=Ketchup]').remove();
      $re.selectpicker('refresh');
    });
    $('.rm-relish').click(function () {
      $re.find('[value=Relish]').remove();
      $re.selectpicker('refresh');
    });
    $('.ex-disable').click(function () {
      $de.prop('disabled', true);
      $de.selectpicker('refresh');
    });
    $('.ex-enable').click(function () {
      $de.prop('disabled', false);
      $de.selectpicker('refresh');
    });
  };
</script>

---

#### `.selectpicker('toggle')`

Programmatically toggles the bootstrap-select menu open/closed.

```js
$('.selectpicker').selectpicker('toggle');
```

---

#### `.selectpicker('hide')`

To programmatically hide the bootstrap-select use the `hide` method (this only affects the visibility of the bootstrap-select itself).

```js
$('.selectpicker').selectpicker('hide');
```

---

#### `.selectpicker('show')`

To programmatically show the bootstrap-select use the `show` method (this only affects the visibility of the bootstrap-select itself).

```js
$('.selectpicker').selectpicker('show');
```

---

#### `.selectpicker('destroy')`

To programmatically destroy the bootstrap-select, use the `destroy` method.

```js
$('.selectpicker').selectpicker('destroy');
```
