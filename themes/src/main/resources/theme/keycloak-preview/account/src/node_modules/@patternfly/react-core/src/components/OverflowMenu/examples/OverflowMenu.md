---
title: Overflow menu
section: components
cssPrefix: 'pf-c-overflow-menu'
typescript: true
propComponents: ['OverflowMenu', 'OverflowMenuContent', 'OverflowMenuControl', 'OverflowMenuDropdownItem', 'OverflowMenuGroup', 'OverflowMenuItem']
---

import { OverflowMenu, OverflowMenuControl, OverflowMenuContent, OverflowMenuGroup, OverflowMenuItem, OverflowMenuDropdownItem } from '@patternfly/react-core';
import { Dropdown, KebabToggle, Button, ButtonVariant } from '@patternfly/react-core';
import { AlignLeftIcon, AlignCenterIcon, AlignRightIcon } from '@patternfly/react-icons';

## Examples
```js title=Simple-(responsive)
import * as React from 'react';
import { OverflowMenu, OverflowMenuControl, OverflowMenuContent, OverflowMenuGroup, OverflowMenuItem, OverflowMenuDropdownItem } from '@patternfly/react-core';
import { Dropdown, KebabToggle } from '@patternfly/react-core';

class SimpleOverflowMenu extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false
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
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <OverflowMenuDropdownItem key="item1" isShared>Item 1</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item2" isShared>Item 2</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item3" isShared>Item 3</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item4" isShared>Item 4</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item5" isShared>Item 5</OverflowMenuDropdownItem>
    ]
    return (
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent>
          <OverflowMenuItem>Item</OverflowMenuItem>
          <OverflowMenuItem>Item</OverflowMenuItem>
          <OverflowMenuGroup>
            <OverflowMenuItem>Item</OverflowMenuItem>
            <OverflowMenuItem>Item</OverflowMenuItem>
            <OverflowMenuItem>Item</OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={this.onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    )
  }
}
```

```js title=Group-types
import * as React from 'react';
import { OverflowMenu, OverflowMenuControl, OverflowMenuContent, OverflowMenuGroup, OverflowMenuItem, OverflowMenuDropdownItem } from '@patternfly/react-core';
import { Dropdown, KebabToggle, Button, ButtonVariant } from '@patternfly/react-core';
import { AlignLeftIcon, AlignCenterIcon, AlignRightIcon } from '@patternfly/react-icons';

class OverflowMenuGroupTypes extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false
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
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <OverflowMenuDropdownItem key="item1" isShared>Item 1</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item2" isShared>Item 2</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="item3" isShared>Item 3</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="primary" isShared>Primary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="secondary" isShared>Secondary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="tertiary" isShared>Tertiary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="action1" isShared>Action 1</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="action2" isShared>Action 2</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="action3" isShared>Action 3</OverflowMenuDropdownItem>,
    ];
    return (
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent>
          <OverflowMenuGroup> 
            <OverflowMenuItem>Item</OverflowMenuItem>
            <OverflowMenuItem>Item</OverflowMenuItem>
            <OverflowMenuItem>Item</OverflowMenuItem>
          </OverflowMenuGroup>
          <OverflowMenuGroup groupType="button">
            <OverflowMenuItem>
              <Button variant={ButtonVariant.primary}>Primary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.secondary}>Secondary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.tertiary}>Tertiary</Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
          <OverflowMenuGroup groupType="icon">
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align left">
                <AlignLeftIcon />
              </Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align center">
                <AlignCenterIcon />
              </Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align right">
                <AlignRightIcon />
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={this.onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    )
  }
}
```


```js title=Multiple-groups
import * as React from 'react';
import { OverflowMenu, OverflowMenuControl, OverflowMenuContent, OverflowMenuGroup, OverflowMenuItem, OverflowMenuDropdownItem } from '@patternfly/react-core';
import { Dropdown, KebabToggle, Button, ButtonVariant } from '@patternfly/react-core';
import { AlignLeftIcon, AlignCenterIcon, AlignRightIcon } from '@patternfly/react-icons';

class OverflowMenuAdditionalOptions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false
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
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <OverflowMenuDropdownItem key="1" isShared>Primary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="2" isShared>Secondary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="3" isShared>Tertiary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="4" isShared>Action 4</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="5" isShared>Action 5</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="6" isShared>Action 6</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="7">Action 7</OverflowMenuDropdownItem>,
    ];
    return (
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent>
          <OverflowMenuGroup groupType="button">
            <OverflowMenuItem>
              <Button variant={ButtonVariant.primary}>Primary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.secondary}>Secondary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.tertiary}>Tertiary</Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
          <OverflowMenuGroup groupType="icon">
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align left">
                <AlignLeftIcon />
              </Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align center">
                <AlignCenterIcon />
              </Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.plain} aria-label="Align right">
                <AlignRightIcon />
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={this.onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    )
  }
}
```


```js title=Persistent
import * as React from 'react';
import { OverflowMenu, OverflowMenuControl, OverflowMenuContent, OverflowMenuGroup, OverflowMenuItem, OverflowMenuDropdownItem } from '@patternfly/react-core';
import { Dropdown, KebabToggle, Button, ButtonVariant } from '@patternfly/react-core';

class OverflowMenuPersist extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false
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
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <OverflowMenuDropdownItem key="secondary" isShared>Secondary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="tertiary" isShared>Tertiary</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="action">Action 4</OverflowMenuDropdownItem>
    ];
    return (
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent isPersistent>
          <OverflowMenuGroup groupType="button" isPersistent>
            <OverflowMenuItem isPersistent>
              <Button variant={ButtonVariant.primary}>Primary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.secondary}>Secondary</Button>
            </OverflowMenuItem>
            <OverflowMenuItem>
              <Button variant={ButtonVariant.tertiary}>Tertiary</Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={this.onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    )
  }
}
```
