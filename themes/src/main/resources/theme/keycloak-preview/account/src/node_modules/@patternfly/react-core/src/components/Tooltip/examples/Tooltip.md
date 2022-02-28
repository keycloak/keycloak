---
title: 'Tooltip'
section: components
cssPrefix: 'pf-c-tooltip'
typescript: true
propComponents: ['Tooltip']
---

import { Button, Tooltip, TooltipPosition, Checkbox } from '@patternfly/react-core';
import { OutlinedQuestionCircleIcon } from '@patternfly/react-icons';

## Examples
```js title=Basic
import React from 'react';
import { Tooltip, TooltipPosition } from '@patternfly/react-core';

<Tooltip
  content={
    <div>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
  }
>
  <span>I have a tooltip!</span>
</Tooltip>
```

```js title=With-left-aligned-text
import React from 'react';
import { Tooltip, TooltipPosition } from '@patternfly/react-core';

<Tooltip
  isContentLeftAligned
  content={
    <div>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
  }
>
  <span>I have a tooltip!</span>
</Tooltip>
```

```js title=On-icon
import React from 'react';
import { Tooltip, TooltipPosition } from '@patternfly/react-core';
import { OutlinedQuestionCircleIcon } from '@patternfly/react-icons';

<div style={{ margin: '50px' }}>
  <Tooltip
    position={TooltipPosition.top}
    content={
      <div>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
    }
  >
    <OutlinedQuestionCircleIcon />
  </Tooltip>
</div>
```

```js title=Positions
import React from 'react';
import { Button, Tooltip, TooltipPosition, Checkbox } from '@patternfly/react-core';

class TooltipPositions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      position: TooltipPosition.top,
      keepInViewChecked: true
    };
    this.handleKeepInViewChange = checked => {
      this.setState({ keepInViewChecked: checked });
    };
  }

  render() {
    return (
      <div>
        <div>
          <span style={{ paddingRight: '10px' }}>Tooltip Position</span>
          <select
            aria-label="Tooltip position"
            onChange={event => {
              this.setState({ position: event.target.value });
            }}
          >
            {Object.keys(TooltipPosition).map(key => (
              <option key={key} value={TooltipPosition[key]}>
                {TooltipPosition[key]}
              </option>
            ))}
          </select>
          <Checkbox
            label="Flip tooltip if the position falls outside the view"
            isChecked={this.state.keepInViewChecked}
            onChange={this.handleKeepInViewChange}
            aria-label="Keep in view"
            id="check-3"
          />
        </div>

        <div style={{ margin: '50px' }}>
          <Tooltip
            position={this.state.position}
            enableFlip={this.state.keepInViewChecked}
            content={
              <div>
                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.
              </div>
            }
          >
            <Button>I have a tooltip!</Button>
          </Tooltip>
        </div>
      </div>
    );
  }
}
```
