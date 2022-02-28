---
title: 'Gallery'
cssPrefix: 'pf-l-gallery'
section: 'layouts'
propComponents: ['Gallery', 'GalleryItem']
typescript: true
---

import { Gallery, GalleryItem } from '@patternfly/react-core';
import './gallery.css';

## Examples
```js title=Basic
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

GalleryBasicExample = () => (
  <Gallery>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
  </Gallery>
);
```

```js title=With-gutters
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

GalleryWithGuttersExample = () => (
  <Gallery gutter="md">
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
    <GalleryItem>Gallery Item</GalleryItem>
  </Gallery>
);
```
