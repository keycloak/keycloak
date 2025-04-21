---
id: Time picker
section: components
cssPrefix: pf-c-time-picker
propComponents: ['TimePicker']
beta: true
---

import { TimePicker } from '@patternfly/react-core';

## Examples

Appending the TimePicker to the `document.body` may cause accessibility issues, including being unable to navigate into the menu via keyboard or other assistive technologies. Instead, appending to the `"parent"` is recommended.

### Basic 12 hour

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

SimpleTimePicker = () => {
  const onChange = (time, hour, minute, seconds, isValid) => {
    console.log('time', time);
    console.log('hour', hour);
    console.log('minute', minute);
    console.log('seconds', seconds);
    console.log('isValid', isValid);
  };

  return <TimePicker time="3:35 AM" onChange={onChange} />;
};
```

### Basic 24 hour

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

<TimePicker time="2020-10-14T18:06:02Z" is24Hour />;
```

### Custom delimiter

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

<TimePicker is24Hour delimiter="h" placeholder="" />;
```

### Minimum/maximum times

The `minTime`/`maxTime` props restrict the options shown for the user to select from as well as trigger the `invalidMinMaxErrorMessage` if the user enters a time outside this range.

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

<TimePicker is24Hour minTime="9:30" maxTime="17:15" placeholder="14:00" />;
```

### With seconds

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

<TimePicker time="3:35:20 PM" includeSeconds />;
```

### Basic 24 hours with seconds

```js
import React from 'react';
import { TimePicker } from '@patternfly/react-core';

<TimePicker time="12:35:50" includeSeconds is24Hour />;
```
