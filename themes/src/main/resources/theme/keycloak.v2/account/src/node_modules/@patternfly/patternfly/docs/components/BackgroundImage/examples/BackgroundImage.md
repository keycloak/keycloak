---
id: Background image
section: components
cssPrefix: pf-c-background-image
---## Examples

### Basic

```html isFullscreen
<div class="pf-c-background-image">
  <svg
    xmlns="http://www.w3.org/2000/svg"
    class="pf-c-background-image__filter"
    width="0"
    height="0"
  >
    <filter id="image_overlay">
      <feColorMatrix
        type="matrix"
        values="1 0 0 0 0
              1 0 0 0 0
              1 0 0 0 0
              0 0 0 1 0"
      />
      <feComponentTransfer color-interpolation-filters="sRGB" result="duotone">
        <feFuncR type="table" tableValues="0.086274509803922 0.43921568627451" />
        <feFuncG type="table" tableValues="0.086274509803922 0.43921568627451" />
        <feFuncB type="table" tableValues="0.086274509803922 0.43921568627451" />
        <feFuncA type="table" tableValues="0 1" />
      </feComponentTransfer>
    </filter>
  </svg>
</div>

```

## Documentation

### Overview

This component puts an image on the background with an svg filter applied to it. The svg must be inline on the page for the filter to work in all browsers.

### Usage

| Class                            | Applied to | Outcome                                                            |
| -------------------------------- | ---------- | ------------------------------------------------------------------ |
| `.pf-c-background-image`         | `*`        | A fixed background image is applied to the background of the page. |
| `.pf-c-background-image__filter` | `*`        | The inline svg that provides the filter for the background image.  |
