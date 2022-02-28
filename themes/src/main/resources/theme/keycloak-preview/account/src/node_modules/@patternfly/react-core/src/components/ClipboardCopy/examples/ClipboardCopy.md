---
title: 'Clipboard copy'
section: components
cssPrefix: 'pf-c-copyclipboard'
propComponents: ['ClipboardCopy']
typescript: true
---

import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

## Examples
```js title=Basic
import React from 'react';
import { ClipboardCopy } from '@patternfly/react-core';

<ClipboardCopy>This is editable</ClipboardCopy>;
```

```js title=Read-Only
import React from 'react';
import { ClipboardCopy } from '@patternfly/react-core';

<ClipboardCopy isReadOnly>This is read-only</ClipboardCopy>;
```

```js title=Expanded
import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

<ClipboardCopy variant={ClipboardCopyVariant.expansion}>
  Got a lot of text here, need to see all of it? Click that arrow on the left side and check out the resulting
  expansion.
</ClipboardCopy>
```
```js title=Read-only-expanded
import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

<ClipboardCopy isReadOnly variant={ClipboardCopyVariant.expansion}>
  Got a lot of text here, need to see all of it? Click that arrow on the left side and check out the resulting
  expansion.
</ClipboardCopy>
```

```js title=Read-only-expanded-by-default
import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

<ClipboardCopy isReadOnly isExpanded variant={ClipboardCopyVariant.expansion}>
  Got a lot of text here, need to see all of it? Click that arrow on the left side and check out the resulting
  expansion.
</ClipboardCopy>
```

```js title=Expanded-with-array
import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

ClipboardCopyArrayOfElements = () => {
  let text = [
    "Got a lot of text here," ,
    "need to see all of it?" ,
    "Click that arrow on the left side and check out the resulting expansion."
  ]
  return <ClipboardCopy variant={ClipboardCopyVariant.expansion}>
    {text.join(" ")}
  </ClipboardCopy>
}
```

```js title=JSON-object-(wrap-code-with-pre)
import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

<ClipboardCopy isCode variant={ClipboardCopyVariant.expansion}>
  

{ `{ "menu": {
  "id": "file",
  "value": "File",
  "popup": {
    "menuitem": [
      {"value": "New", "onclick": "CreateNewDoc()"},
      {"value": "Open", "onclick": "OpenDoc()"},
      {"value": "Close", "onclick": "CloseDoc()"}
    ]
  }
}} `}
  
</ClipboardCopy>
```
