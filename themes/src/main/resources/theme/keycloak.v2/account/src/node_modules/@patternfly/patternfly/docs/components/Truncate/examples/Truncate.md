---
id: 'Truncate'
beta: true
section: components
cssPrefix: pf-c-truncate
---import './Truncate.css'

## Examples

### Notes

The truncate component contains two child elements, `.pf-c-truncate__start` and `.pf-c-truncate__end`. If both `start` and `end` are present within `.pf-c-truncate`, trucation will occur in the middle of the string. If only `.pf-c-truncate__start` is present, truncation will occur at the end of the string. If only `.pf-c-truncate__end` is present, truncation will occur at the beginning of the string. A `.pf-c-popover` will be automatically applied to the PatternFly React implementation. `&lrm;` must be included at the end of string to denote the ending punctuation mark. Otherwise it will occur and the beggining of truncation for a `pf-c-truncate__end` element.

### Default

```html
<div class="pf-c-truncate--example">
  <span class="pf-c-truncate">
    <span
      class="pf-c-truncate__start"
    >Vestibulum interdum risus et enim faucibus, sit amet molestie est accumsan.</span>
  </span>
</div>

```

### Middle

```html
<div class="pf-c-truncate--example">
  <span class="pf-c-truncate">
    <span
      class="pf-c-truncate__start"
    >redhat_logo_black_and_white_reversed_simple_with_fedora_con</span>
    <span class="pf-c-truncate__end">tainer.zip</span>
  </span>
</div>

```

### Start

```html
<div class="pf-c-truncate--example">
  <span class="pf-c-truncate">
    <span
      class="pf-c-truncate__end"
    >Vestibulum interdum risus et enim faucibus, sit amet molestie est accumsan.&lrm;</span>
  </span>
</div>

```

## Documentation

### Usage

| Class                   | Applied  | Outcome                                       |
| ----------------------- | -------- | --------------------------------------------- |
| `.pf-c-truncate`        | `<span>` | Initiates the truncate component.             |
| `.pf-c-truncate__start` | `<span>` | Defines the truncate component starting text. |
| `.pf-c-truncate__end`   | `<span>` | Defines the truncate component ending text.   |
