---
title: 'Dropdown'
section: components
cssPrefix: 'pf-c-dropdown'
propComponents:
  ['Dropdown', 'DropdownGroup', 'DropdownItem', 'DropdownToggle', 'DropdownToggleCheckbox', 'DropdownToggleAction']
typescript: true
---

import { Dropdown, DropdownToggle, DropdownToggleCheckbox, DropdownItem, DropdownItemIcon, DropdownSeparator, DropdownPosition, DropdownDirection, KebabToggle, DropdownGroup, DropdownToggleAction } from '@patternfly/react-core';
import { ThIcon, CaretDownIcon, CogIcon, BellIcon, CubesIcon } from '@patternfly/react-icons';
import { Link } from '@reach/router';

## Examples

```js title=Basic
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon, CaretDownIcon } from '@patternfly/react-icons';

class SimpleDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle id="toggle-id" onToggle={this.onToggle} iconComponent={CaretDownIcon}>
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=With-initial-selection
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class IntialSelectionDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-1');
      element.focus();
    };
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button" autoFocus>
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle id="toggle-id-1" onToggle={this.onToggle}>
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
        autoFocus={false}
      />
    );
  }
}
```

```js title=With-groups
import React from 'react';
import { Dropdown, DropdownToggle, DropdownGroup, DropdownItem, DropdownSeparator } from '@patternfly/react-core';

class GroupedDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-3');
      element.focus();
    };
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownGroup key="group 1">
        <DropdownItem key="group 1 link">Link</DropdownItem>
        <DropdownItem key="group 1 action" component="button">
          Action
        </DropdownItem>
      </DropdownGroup>,
      <DropdownGroup label="Group 2" key="group 2">
        <DropdownItem key="group 2 link">Group 2 Link</DropdownItem>
        <DropdownItem key="group 2 action" component="button">
          Group 2 Action
        </DropdownItem>
      </DropdownGroup>,
      <DropdownGroup label="Group 3" key="group 3">
        <DropdownItem key="group 3 link">Group 3 Link</DropdownItem>
        <DropdownItem key="group 3 action" component="button">
          Group 3 Action
        </DropdownItem>
      </DropdownGroup>
    ];
    return (
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle onToggle={this.onToggle} id="toggle-id-3">
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
        isGrouped
      />
    );
  }
}
```

```js title=Disabled
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class DisabledDropdown extends React.Component {
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle isDisabled onToggle={this.onToggle}>
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Primary-toggle
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection
} from '@patternfly/react-core';
import { ThIcon, CaretDownIcon } from '@patternfly/react-icons';

class PrimaryDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-4');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle onToggle={this.onToggle} iconComponent={CaretDownIcon} isPrimary id="toggle-id-4">
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Position-right
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class PositionRightDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-5');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        position={DropdownPosition.right}
        toggle={<DropdownToggle onToggle={this.onToggle}>Dropdown</DropdownToggle>}
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Direction-up
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class DirectionUpDropdown extends React.Component {
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
      <Dropdown
        direction={DropdownDirection.up}
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle onToggle={this.onToggle} id="toggle-id-5">
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=With-kebab
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class KebabDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-6');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={<KebabToggle onToggle={this.onToggle} id="toggle-id-6" />}
        isOpen={isOpen}
        isPlain
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Icon-only
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class IconDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-7');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle iconComponent={null} onToggle={this.onToggle} aria-label="Applications" id="toggle-id-7">
            <ThIcon />
          </DropdownToggle>
        }
        isOpen={isOpen}
        isPlain
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Split-button
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleCheckbox,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class SplitButtonDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-8');
      element.focus();
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle
            splitButtonItems={[
              <DropdownToggleCheckbox id="example-checkbox-1" key="split-checkbox" aria-label="Select all" />
            ]}
            onToggle={this.onToggle}
            id="toggle-id-8"
          />
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Split-button-(with-text)
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleCheckbox,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class SplitButtonDropdown extends React.Component {
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle
            splitButtonItems={[
              <DropdownToggleCheckbox id="example-checkbox-2" key="split-checkbox" aria-label="Select all">
                10 selected
              </DropdownToggleCheckbox>
            ]}
            onToggle={this.onToggle}
          />
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Split-button-(3rd-state)
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleCheckbox,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';
class SplitButtonDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      isChecked: null
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
    this.onChange = isChecked => {
      this.setState({
        isChecked
      });
    };
  }
  render() {
    const { isOpen, isChecked } = this.state;
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle
            splitButtonItems={[
              <DropdownToggleCheckbox
                id="example-checkbox-3rd-state"
                key="split-checkbox"
                aria-label="Select all"
                onChange={checked => this.onChange(checked)}
                isChecked={isChecked}
              />
            ]}
            onToggle={this.onToggle}
          />
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Split-button-(disabled)
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleCheckbox,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class SplitButtonDisabledDropdown extends React.Component {
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
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle
            splitButtonItems={[
              <DropdownToggleCheckbox
                id="example-checkbox-3"
                key="disabled-checkbox"
                aria-label="Select all"
                isDisabled
              />
            ]}
            isDisabled
            onToggle={this.onToggle}
          />
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```

```js title=Split-button-action
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleAction,
  DropdownItem,
  DropdownItemIcon,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon, CogIcon, BellIcon, CubesIcon } from '@patternfly/react-icons';

class SplitButtonActionDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isActionOpen: false,
      isCogOpen: false
    };
    this.onActionToggle = isActionOpen => {
      this.setState({
        isActionOpen
      });
    };
    this.onCogToggle = isCogOpen => {
      this.setState({
        isCogOpen
      });
    };
    this.onActionClick = event => {
      console.log('Action clicked!');
    };
    this.onCogClick = event => {
      console.log('Cog clicked!');
    };
    this.onActionSelect = event => {
      this.setState({
        isActionOpen: !this.state.isActionOpen
      });
    };
    this.onCogSelect = event => {
      this.setState({
        isCogOpen: !this.state.isCogOpen
      });
    };
  }

  render() {
    const { isActionOpen, isCogOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" component="button" isDisabled>
        Disabled action
      </DropdownItem>,
      <DropdownItem key="other action" component="button">
        Other action
      </DropdownItem>
    ];
    const dropdownIconItems = [
      <DropdownItem key="action" component="button" variant="icon">
        <DropdownItemIcon>
          <CogIcon />
        </DropdownItemIcon>
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" component="button" variant="icon" isDisabled>
        <DropdownItemIcon>
          <BellIcon />
        </DropdownItemIcon>
        Disabled action
      </DropdownItem>,
      <DropdownItem key="other action" component="button" variant="icon">
        <DropdownItemIcon>
          <CubesIcon />
        </DropdownItemIcon>
        Other action
      </DropdownItem>
    ];
    return (
      <React.Fragment>
        <Dropdown
          onSelect={this.onActionSelect}
          toggle={
            <DropdownToggle
              splitButtonItems={[
                <DropdownToggleAction key="action" onClick={this.onActionClick}>
                  Action
                </DropdownToggleAction>
              ]}
              splitButtonVariant="action"
              onToggle={this.onActionToggle}
            />
          }
          isOpen={isActionOpen}
          dropdownItems={dropdownItems}
        />{' '}
        <Dropdown
          onSelect={this.onCogSelect}
          toggle={
            <DropdownToggle
              splitButtonItems={[
                <DropdownToggleAction key="cog-action" aria-label="Action" onClick={this.onCogClick}>
                  <CogIcon />
                </DropdownToggleAction>
              ]}
              splitButtonVariant="action"
              onToggle={this.onCogToggle}
            />
          }
          isOpen={isCogOpen}
          dropdownItems={dropdownIconItems}
        />
      </React.Fragment>
    );
  }
}
```

```js title=Basic-panel
import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { ThIcon } from '@patternfly/react-icons';

class DropdownPanel extends React.Component {
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
    return (
      <Dropdown
        onSelect={this.onSelect}
        toggle={<DropdownToggle onToggle={this.onToggle}>Expanded Dropdown</DropdownToggle>}
        isOpen={isOpen}
      >
        [Panel contents here]
      </Dropdown>
    );
  }
}
```

```js title=Router-link
import React from 'react';
import {
  Button,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownDirection,
  KebabToggle
} from '@patternfly/react-core';
import { Link } from '@reach/router';
import { ThIcon, CaretDownIcon } from '@patternfly/react-icons';

class RouterDropdown extends React.Component {
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
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id');
      element.focus();
    };
  }

  render() {
    const { isOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="routerlink" component={
        <Link to="/">
          @reach/router Link
        </Link>
      } />
    ];

    return (
      <Dropdown
        onSelect={this.onSelect}
        toggle={
          <DropdownToggle id="toggle-id" onToggle={this.onToggle} iconComponent={CaretDownIcon}>
            Dropdown
          </DropdownToggle>
        }
        isOpen={isOpen}
        dropdownItems={dropdownItems}
      />
    );
  }
}
```
