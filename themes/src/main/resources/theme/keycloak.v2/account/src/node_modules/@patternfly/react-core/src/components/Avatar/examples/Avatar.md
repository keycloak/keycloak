---
id: Avatar
section: components
cssPrefix: pf-c-avatar
propComponents: ['Avatar']
---

import avatarImg from './avatarImg.svg';
import avatarImgDark from './avatarImgDark.svg';
import './example.css';

## Examples

### Basic

```ts
import React from 'react';
import { Avatar } from '@patternfly/react-core';
import avatarImg from './avatarImg.svg';

<Avatar src={avatarImg} alt="avatar" />;
```

### Size variations

```ts
import React from 'react';
import { Avatar } from '@patternfly/react-core';
import avatarImg from './avatarImg.svg';

<React.Fragment>
  Small
  <br />
  <Avatar src={avatarImg} alt="avatar" size="sm" />
  <br />
  Medium
  <br />
  <Avatar src={avatarImg} alt="avatar" size="md" />
  <br />
  Large
  <br />
  <Avatar src={avatarImg} alt="avatar" size="lg" />
  <br />
  Extra Large
  <br />
  <Avatar src={avatarImg} alt="avatar" size="xl" />
</React.Fragment>;
```

### Bordered - light

```ts
import React from 'react';
import { Avatar } from '@patternfly/react-core';
import avatarImg from './img_avatar.svg';

<Avatar src={avatarImg} alt="avatar" border="light" />;
```

### Bordered - dark

```ts
import React from 'react';
import { Avatar } from '@patternfly/react-core';
import avatarImgDark from './img_avatar-dark.svg';

<Avatar src={avatarImgDark} alt="avatar" border="dark" />;
```
