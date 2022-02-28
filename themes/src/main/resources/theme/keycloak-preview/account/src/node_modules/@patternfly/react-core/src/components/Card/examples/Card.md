---
title: 'Card'
section: components
cssPrefix: 'pf-c-card'
typescript: true
propComponents: ['Card', 'CardHeadMain', 'CardHeader', 'CardBody', 'CardFooter']
---

import { Brand, Card, CardActions, CardHead, CardHeadMain, CardHeader, CardBody, CardFooter, Checkbox, DropdownActions } from '@patternfly/react-core';
import pfLogo from './pfLogo.svg'; 

## Examples
```js title=Basic
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

SimpleCard = () => (
  <Card>
    <CardHeader>Header</CardHeader>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=With-image-and-actions
import React from 'react'; 
import { Brand, Card, CardHead, CardHeadMain, CardActions, CardHeader, CardBody, CardFooter, Dropdown, DropdownToggle, DropdownItem, DropdownSeparator, DropdownPosition, DropdownDirection, KebabToggle, } from '@patternfly/react-core'; 
import pfLogo from './pfLogo.svg'; 

class KebabDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      check1: false
    };
    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };
    this.onSelect = event => {
      this.setState({
        isOpen: !this.state.isOpen
      });
    };
    this.onClick = (checked, event) => {
      const target = event.target; 
      const value = target.type === 'checkbox' ? target.checked : target.value; 
      const name = target.name; 
      this.setState({ [name]: value }); 
    }; 
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>,
      <DropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];
    return (
      <Card>
        <CardHead>
          <CardHeadMain>
            <Brand src={pfLogo} alt="PatternFly logo" style={{ height: '50px' }}/>
          </CardHeadMain>
          <CardActions>
            <Dropdown
              onSelect={this.onSelect}
              toggle={<KebabToggle onToggle={this.onToggle} />}
              isOpen={isOpen}
              isPlain
              dropdownItems={dropdownItems}
              position={'right'}
            />
            <input
              type="checkbox" 
              isChecked={this.state.check1}
              onChange={this.onClick}
              aria-label="card checkbox example"
              id="check-1"
              name="check1"
            />
          </CardActions>
        </CardHead>
        <CardHeader>Header</CardHeader>
        <CardBody>Body</CardBody>
        <CardFooter>Footer</CardFooter>
      </Card>
    );
  }
}
```

```js title=Card-header-in-card-head
 import React from 'react'; 
import { Card, CardHead, CardActions, CardHeader, CardBody, CardFooter, Dropdown, DropdownToggle, DropdownItem, DropdownSeparator, DropdownPosition, DropdownDirection, KebabToggle } from '@patternfly/react-core'; 

class KebabDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      check1: false
    };
    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };
    this.onSelect = event => {
      this.setState({
        isOpen: !this.state.isOpen
      });
    };
    this.onClick = (checked, event) => {
      const target = event.target; 
      const value = target.type === 'checkbox' ? target.checked : target.value; 
      const name = target.name; 
      this.setState({ [name]: value }); 
    }; 
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>,
      <DropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];
    return (
      <Card>
        <CardHead>
          <CardActions>
            <Dropdown
              onSelect={this.onSelect}
              toggle={<KebabToggle onToggle={this.onToggle} />}
              isOpen={isOpen}
              isPlain
              dropdownItems={dropdownItems}
              position={'right'}
            />
            <input
              type="checkbox" 
              isChecked={this.state.check1}
              onChange={this.onClick}
              aria-label="card checkbox example"
              id="check-1"
              name="check1"
            />
          </CardActions>
        <CardHeader>This is a really really really really really really really really really really long header</CardHeader>
        </CardHead>
        <CardBody>Body</CardBody>
        <CardFooter>Footer</CardFooter>
      </Card>
    );
  }
}
```

```js title=Only-actions-in-card-head-(no-header/footer)
 import React from 'react'; 
import { Dropdown, DropdownToggle, DropdownItem, DropdownSeparator, DropdownPosition, DropdownDirection, KebabToggle, Card, CardHead, CardActions, CardHeader, CardBody } from '@patternfly/react-core'; 

class KebabDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      check1: false
    };
    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };
    this.onSelect = event => {
      this.setState({
        isOpen: !this.state.isOpen
      });
    };
    this.onClick = (checked, event) => {
      const target = event.target; 
      const value = target.type === 'checkbox' ? target.checked : target.value; 
      const name = target.name; 
      this.setState({ [name]: value }); 
    }; 
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>,
      <DropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];
    return (
      <Card>
        <CardHead>
          <CardActions>
            <Dropdown
              onSelect={this.onSelect}
              toggle={<KebabToggle onToggle={this.onToggle} />}
              isOpen={isOpen}
              isPlain
              dropdownItems={dropdownItems}
              position={'right'}
            />
            <input
              type="checkbox" 
              isChecked={this.state.check1}
              onChange={this.onClick}
              aria-label="card checkbox example"
              id="check-1"
              name="check1"
            />
          </CardActions>
        </CardHead>
        <CardBody>This is the card body, there is only actions in the card head.</CardBody>
      </Card>
    );
  }
}
```

```js title=Only-image-in-the-card-head
import React from 'react';
import { Brand, Card, CardBody, CardFooter, CardHead, CardHeadMain, CardHeader } from '@patternfly/react-core';

ImageCard = () => (
  <Card>
    <CardHead>
      <CardHeadMain>
        <Brand src={pfLogo} alt="PatternFly logo" style={{ height: '50px' }}/>
      </CardHeadMain>
    </CardHead> 
    <CardHeader>Header</CardHeader>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=With-no-footer
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

NoFooterCard = () => (
  <Card>
    <CardHeader>Header</CardHeader>
    <CardBody>This card has no footer</CardBody>
  </Card>
);
```

```js title=With-no-header
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

NoHeaderCard = () => (
  <Card>
    <CardBody>This card has no header</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=With-only-a-content-section
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

ContentOnlyCard = () => (
  <Card>
    <CardBody>Body</CardBody>
  </Card>
);
```

```js title=With-multiple-body-sections
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

MultipleBodyCard = () => (
  <Card>
    <CardHeader>Header</CardHeader>
    <CardBody>Body</CardBody>
    <CardBody>Body</CardBody>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=With-only-one-body-that-fills
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

NoFillBodyCard = () => (
  <Card style={{ minHeight: '30em' }}>
    <CardHeader>Header</CardHeader>
    <CardBody isFilled={false}>Body pf-m-no-fill</CardBody>
    <CardBody isFilled={false}>Body pf-m-no-fill</CardBody>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=Hover
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

HoverableCard = () => (
  <Card isHoverable>
    <CardHeader>Header</CardHeader>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=Compact
import React from 'react';
import { Card, CardHeader, CardBody, CardFooter } from '@patternfly/react-core';

HoverableCard = () => (
  <Card isCompact>
    <CardHeader>Header</CardHeader>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
```

```js title=Selectable-and-selected
import React from 'react';
import { Card, CardHead, CardActions, CardHeader, CardBody, Dropdown, DropdownToggle, DropdownItem, DropdownSeparator, DropdownPosition, DropdownDirection, KebabToggle, } from '@patternfly/react-core'; 

class SelectableCard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selected: null
    };
    this.onKeyDown = event => {
        if (event.target !== event.currentTarget) {
          return;
        }
        if ([13, 32].includes(event.keyCode)) {
          const newSelected = event.currentTarget.id === this.state.selected ? null : event.currentTarget.id
          this.setState({
            selected: newSelected
          })
        }
    }
    this.onClick = event => {
      const newSelected = event.currentTarget.id === this.state.selected ? null : event.currentTarget.id
      this.setState({
        selected: newSelected
      })
    }; 
    this.onToggle = (isOpen, event) => {
      event.stopPropagation()
      this.setState({
        isOpen
      });
    };
    this.onSelect = event => {
      event.stopPropagation()
      this.setState({
        isOpen: !this.state.isOpen
      });
    };
  }
  render() {
    const { selected, isOpen} = this.state
    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>,
      <DropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];
    return (
      <>
      <Card id="first-card" onKeyDown={this.onKeyDown} onClick={this.onClick} isSelectable isSelected={selected === 'first-card'}>
        <CardHead>
          <CardActions>
            <Dropdown
              onSelect={this.onSelect}
              toggle={<KebabToggle onToggle={this.onToggle} />}
              isOpen={isOpen}
              isPlain
              dropdownItems={dropdownItems}
              position={'right'}
            />
          </CardActions>
        </CardHead>
        <CardHeader>First card</CardHeader>
        <CardBody>This is a selectable card. Click me to select me. Click again to deselect me.</CardBody>
      </Card>
      <br/>
      <Card id="second-card" onKeyDown={this.onKeyDown} onClick={this.onClick} isSelectable isSelected={selected === 'second-card'}>
        <CardHeader>Second card</CardHeader>
        <CardBody>This is a selectable card. Click me to select me. Click again to deselect me.</CardBody>
      </Card>
      </>
    );
  }
}
```
