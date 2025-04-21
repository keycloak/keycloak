---
id: Gallery
section: layouts
cssPrefix: pf-l-gallery
---import './Gallery.css'

## Examples

### Basic

```html
<div class="pf-l-gallery">
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### With gutter

```html
<div class="pf-l-gallery pf-m-gutter">
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### Grid template minmax() width customization

Grid template columms min value can be applied to <code>.pf-l-gallery</code>.  Min-width is set inline as `--pf-l-gallery--GridTemplateColumns--min{-on-[breakpoint]}: {width}`.

### Min width modified gallery

```html
<div
  class="pf-l-gallery pf-m-gutter"
  style="--pf-l-gallery--GridTemplateColumns--min: 150px;"
>
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### Max width modified gallery

```html
<div
  class="pf-l-gallery pf-m-gutter"
  style="--pf-l-gallery--GridTemplateColumns--max: 300px;"
>
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### Min width modified gallery, responsive updates

```html
<div
  class="pf-l-gallery pf-m-gutter"
  style="--pf-l-gallery--GridTemplateColumns--min-on-md: 100px; --pf-l-gallery--GridTemplateColumns--min-on-lg: 150px; --pf-l-gallery--GridTemplateColumns--min-on-xl: 200px; --pf-l-gallery--GridTemplateColumns--min-on-2xl: 300px;"
>
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### Max width modified gallery, responsive updates

```html
<div
  class="pf-l-gallery pf-m-gutter"
  style="--pf-l-gallery--GridTemplateColumns--max-on-md: 280px; --pf-l-gallery--GridTemplateColumns--max-on-lg: 320px; --pf-l-gallery--GridTemplateColumns--max-on-2xl: 400px;"
>
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### Min and max width modified gallery, responsive updates

```html
<div
  class="pf-l-gallery pf-m-gutter"
  style="--pf-l-gallery--GridTemplateColumns--min: 100%; --pf-l-gallery--GridTemplateColumns--min-on-md: 100px; --pf-l-gallery--GridTemplateColumns--max-on-md: 200px; --pf-l-gallery--GridTemplateColumns--min-on-xl: 300px; --pf-l-gallery--GridTemplateColumns--max-on-xl: 1fr;"
>
  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>

  <div class="pf-l-gallery__item">content</div>
</div>

```

### List type

```html
<ul class="pf-l-gallery pf-m-gutter">
  <li class="pf-l-gallery__item">content</li>

  <li class="pf-l-gallery__item">content</li>

  <li class="pf-l-gallery__item">content</li>

  <li class="pf-l-gallery__item">content</li>

  <li class="pf-l-gallery__item">content</li>
</ul>

```

## Documentation

### Overview

The gallery layout is designed so that all of its children are of uniform size, display horizontally, and wrap responsively.

### Usage

| Class                                                                 | Applied to      | Outcome                                                                                                                                                                                                |
| --------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-l-gallery`                                                       | `<div>`         | Initializes a Gallery layout                                                                                                                                                                           |
| `.pf-l-gallery__item`                                                 | `<div>`         | Explicitly sets the child for the gallery. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers. |
| `.pf-m-gutter`                                                        | `.pf-l-gallery` | Adds space between children by using the globally defined gutter value.                                                                                                                                |
| `--pf-l-gallery--GridTemplateColumns--min{-on-[breakpoint]}: {width}` | `.pf-l-gallery` | Modifies the min value of `grid-template-columns` declaration at the optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                         |
| `--pf-l-gallery--GridTemplateColumns--max{-on-[breakpoint]}: {width}` | `.pf-l-gallery` | Modifies the max value of `grid-template-columns` declaration at the optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                         |
