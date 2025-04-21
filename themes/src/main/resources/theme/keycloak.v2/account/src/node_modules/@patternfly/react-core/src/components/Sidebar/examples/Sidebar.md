---
id: Sidebar
cssPrefix: pf-c-sidebar
section: components
propComponents: ['Sidebar', 'SidebarContent', 'SidebarPanel']
---

## Examples
### Basic
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar>
  <SidebarPanel>
    Sidebar panel
  </SidebarPanel>
  <SidebarContent>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```

### Stack
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar orientation="stack">
  <SidebarPanel>
    Sidebar panel stacked on top
  </SidebarPanel>
  <SidebarContent>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```

### Panel right with gutter
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar isPanelRight hasGutter>
  <SidebarPanel>
    Sidebar panel on the right
  </SidebarPanel>
  <SidebarContent>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```

### Sticky panel
A `tabIndex` should be added to the `Sidebar` when there is scrollable content in order for the  overflow content to be accessible by keyboard.
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar style={{ height: '200px', overflow: 'auto' }} tabIndex={0}>
  <SidebarPanel variant="sticky">
    Sticky sidebar panel
  </SidebarPanel>
  <SidebarContent>
    <p>Scroll me!</p>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```

### Static panel
A `tabIndex` should be added to the `Sidebar` when there is scrollable content in order for the  overflow content to be accessible by keyboard.
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar style={{ height: '200px', overflow: 'auto' }} tabIndex={0}>
  <SidebarPanel variant="static">
    Static sidebar panel
  </SidebarPanel>
  <SidebarContent>
    <p>Scroll me!</p>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```

### Responsive panel width
A `tabIndex` should be added to the `Sidebar` when there is scrollable content in order for the  overflow content to be accessible by keyboard.
```js
import React from 'react';
import { Sidebar, SidebarContent, SidebarPanel } from '@patternfly/react-core';

<Sidebar style={{ height: '200px', overflow: 'auto' }} tabIndex={0}>
  <SidebarPanel width={{ default: 'width_50', lg: 'width_33', xl: 'width_75' }}>
    Sidebar panel
  </SidebarPanel>
  <SidebarContent>
    <p>Resize me!</p>
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse dapibus nulla id augue dictum commodo. Donec mollis arcu massa, sollicitudin venenatis est rutrum vitae. Integer pulvinar ligula at augue mollis, ac pulvinar arcu semper. Maecenas nisi lorem, malesuada ac lectus nec, porta pretium neque. Ut convallis libero sit amet metus mattis, vel facilisis lorem malesuada. Duis consectetur ante sit amet magna efficitur, a interdum leo vulputate.</p>
    <p>Praesent at odio nec sapien ultrices tincidunt in non mauris. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Duis consectetur nisl quis facilisis faucibus. Sed eu bibendum risus. Suspendisse porta euismod tortor, at elementum odio suscipit sed. Cras eget ultrices urna, ac feugiat lectus. Integer a pharetra velit, in imperdiet mi. Phasellus vel hendrerit velit. Vestibulum ut augue vitae erat vulputate bibendum a ut magna.</p>
  </SidebarContent>
</Sidebar>
```


