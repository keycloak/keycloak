---
id: Gallery
cssPrefix: pf-l-gallery
section: layouts
propComponents: ['Gallery', 'GalleryItem']
---

import './gallery.css';

## Examples

### Basic

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
</Gallery>;
```

### With gutters

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery hasGutter>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
</Gallery>;
```

### Adjusting min widths

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery
  hasGutter
  minWidths={{
    md: '100px',
    lg: '150px',
    xl: '200px',
    '2xl': '300px'
  }}
>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
</Gallery>;
```

### Adjusting max widths

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery
  hasGutter
  maxWidths={{
    md: '280px',
    lg: '320px',
    '2xl': '400px'
  }}
>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
</Gallery>;
```

### Adjusting min and max widths

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery
  hasGutter
  minWidths={{
    default: '100%',
    md: '100px',
    xl: '300px'
  }}
  maxWidths={{
    md: '200px',
    xl: '1fr'
  }}
>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
  <GalleryItem>Gallery Item</GalleryItem>
</Gallery>;
```

### Alternative components

```js
import React from 'react';
import { Gallery, GalleryItem } from '@patternfly/react-core';

<Gallery component='ul'>
  <GalleryItem component='li'>Gallery item</GalleryItem>
  <GalleryItem component='li'>Gallery item</GalleryItem>
  <GalleryItem component='li'>Gallery item</GalleryItem>
  <GalleryItem component='li'>Gallery item</GalleryItem>
  <GalleryItem component='li'>Gallery item</GalleryItem>
</Gallery>
```
