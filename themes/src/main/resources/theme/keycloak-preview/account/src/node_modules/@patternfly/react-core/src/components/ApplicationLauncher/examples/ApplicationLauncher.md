---
title: 'Application launcher'
section: components
cssPrefix: 'pf-c-app-launcher'
propComponents: ['ApplicationLauncher', 'ApplicationLauncherItem']
typescript: true
---

import { ApplicationLauncher, ApplicationLauncherContent, ApplicationLauncherIcon, ApplicationLauncherText, ApplicationLauncherItem, ApplicationLauncherGroup, ApplicationLauncherSeparator, Text } from '@patternfly/react-core';
import { HelpIcon, StarIcon } from '@patternfly/react-icons';
import { Link } from '@reach/router';
import pfIcon from './pf-logo-small.svg';

Note: Application launcher is built on Dropdown, for extended API go to [Dropdown](/documentation/react/components/dropdown) documentation.
To add a tooltip, use the `tooltip` prop and optionally add more tooltip props by using `tooltipProps`. For more tooltip information go to [Tooltip](/documentation/react/components/tooltip).


## Examples

```js title=Basic
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';

class SimpleApplicationLauncher extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher onSelect={this.onSelect} onToggle={this.onToggle} isOpen={isOpen} items={appLauncherItems} />
    );
  }
}
```

```js title=Router-link
import React from 'react';
import { Link } from '@reach/router';
import { ApplicationLauncher, ApplicationLauncherItem, ApplicationLauncherContent, Text } from '@patternfly/react-core';

class SimpleApplicationLauncher extends React.Component {
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
    const icon = <img src={pfIcon} />;
    const exampleStyle = {
      color: 'var(--pf-c-app-launcher__menu-item--Color)',
      textDecoration: 'none'
    };
    const appLauncherItems = [
      <ApplicationLauncherItem
        key="router1"
        component={
          <Link to="/" style={exampleStyle}>
            @reach/router Link
          </Link>
        }
      />,
      <ApplicationLauncherItem
        key="router2"
        isExternal
        icon={icon}
        component={
          <Link to="/" style={exampleStyle}>
            <ApplicationLauncherContent>@reach/router Link with icon</ApplicationLauncherContent>
          </Link>
        }
      />,
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher onSelect={this.onSelect} onToggle={this.onToggle} isOpen={isOpen} items={appLauncherItems} />
    );
  }
}
```

```js title=Disabled
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';

class SimpleApplicationLauncher extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher
        onSelect={this.onSelect}
        onToggle={this.onToggle}
        isOpen={isOpen}
        items={appLauncherItems}
        isDisabled
      />
    );
  }
}
```

```js title=Aligned-right
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';
import { DropdownPosition } from '../Dropdown';

class SimpleApplicationLauncher extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    const style = { marginLeft: 'calc(100% - 46px)' };
    return (
      <ApplicationLauncher
        onSelect={this.onSelect}
        onToggle={this.onToggle}
        isOpen={isOpen}
        items={appLauncherItems}
        position={DropdownPosition.right}
        style={style}
      />
    );
  }
}
```

```js title=Aligned-top
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';
import { DropdownDirection } from '../Dropdown';

class SimpleApplicationLauncher extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher
        onSelect={this.onSelect}
        onToggle={this.onToggle}
        isOpen={isOpen}
        items={appLauncherItems}
        direction={DropdownDirection.up}
      />
    );
  }
}
```

```js title=With-tooltip
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';

class TooltipApplicationLauncher extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1b" href="#" tooltip={<div>Launch Application 1</div>}>
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem
        key="application_2b"
        component="button"
        tooltip={<div>Launch Application 2</div>}
        tooltipProps={{ position: 'right' }}
        onClick={() => alert('Clicked item 2')}
      >
        Application 2 (onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem
        key="application_3b"
        component="button"
        tooltip={<div>Launch Application 3</div>}
        tooltipProps={{ position: 'bottom' }}
        onClick={() => alert('Clicked item 3')}
      >
        Application 3 (onClick)
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher onSelect={this.onSelect} onToggle={this.onToggle} isOpen={isOpen} items={appLauncherItems} />
    );
  }
}
```

```js title=With-sections-and-icons
import React from 'react';
import {
  ApplicationLauncher,
  ApplicationLauncherIcon,
  ApplicationLauncherText,
  ApplicationLauncherItem,
  ApplicationLauncherGroup,
  ApplicationLauncherSeparator
} from '@patternfly/react-core';
import pfIcon from './examples/pf-logo-small.svg';

class ApplicationLauncherSections extends React.Component {
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
    const icon = <img src={pfIcon} />;
    const appLauncherItems = [
      <ApplicationLauncherGroup key="group 1c">
        <ApplicationLauncherItem key="group 1a" icon={icon}>
          Item without group title
        </ApplicationLauncherItem>
        <ApplicationLauncherSeparator key="separator" />
      </ApplicationLauncherGroup>,
      <ApplicationLauncherGroup label="Group 2" key="group 2c">
        <ApplicationLauncherItem key="group 2a" isExternal icon={icon} component="button">
          Group 2 button
        </ApplicationLauncherItem>
        <ApplicationLauncherItem key="group 2b" isExternal href="#" icon={icon}>
          Group 2 anchor link
        </ApplicationLauncherItem>
        <ApplicationLauncherSeparator key="separator" />
      </ApplicationLauncherGroup>,
      <ApplicationLauncherGroup label="Group 3" key="group 3c">
        <ApplicationLauncherItem key="group 3a" isExternal icon={icon} component="button">
          Group 3 button
        </ApplicationLauncherItem>
        <ApplicationLauncherItem key="group 3b" isExternal href="#" icon={icon}>
          Group 3 anchor link
        </ApplicationLauncherItem>
      </ApplicationLauncherGroup>
    ];
    return (
      <ApplicationLauncher
        onSelect={this.onSelect}
        onToggle={this.onToggle}
        isOpen={isOpen}
        items={appLauncherItems}
        isGrouped
      />
    );
  }
}
```

```js title=With-favorites-and-search
import React from 'react';
import {
  ApplicationLauncher,
  ApplicationLauncherItem,
  ApplicationLauncherGroup,
  ApplicationLauncherSeparator
} from '@patternfly/react-core';
import pfIcon from './examples/pf-logo-small.svg';

class ApplicationLauncherFavorites extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      favorites: [],
      filteredItems: null
    };

    const icon = <img src={pfIcon} />;
    this.appLauncherItems = [
      <ApplicationLauncherGroup key="group 1c">
        <ApplicationLauncherItem key="group 1a" id="item-1" icon={icon}>
          Item without group title
        </ApplicationLauncherItem>
        <ApplicationLauncherSeparator key="separator" />
      </ApplicationLauncherGroup>,
      <ApplicationLauncherGroup label="Group 2" key="group 2c">
        <ApplicationLauncherItem key="group 2a" id="item-2" isExternal icon={icon} component="button">
          Group 2 button
        </ApplicationLauncherItem>
        <ApplicationLauncherItem key="group 2b" id="item-3" isExternal href="#" icon={icon}>
          Group 2 anchor link
        </ApplicationLauncherItem>
        <ApplicationLauncherSeparator key="separator" />
      </ApplicationLauncherGroup>,
      <ApplicationLauncherGroup label="Group 3" key="group 3c">
        <ApplicationLauncherItem key="group 3a" id="item-4" isExternal icon={icon} component="button">
          Group 3 button
        </ApplicationLauncherItem>
        <ApplicationLauncherItem key="group 3b" id="item-5" isExternal href="#" icon={icon}>
          Group 3 anchor link
        </ApplicationLauncherItem>
      </ApplicationLauncherGroup>
    ];

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
    this.onFavorite = (itemId, isFavorite) => {
      if (isFavorite) {
        this.setState({
          favorites: this.state.favorites.filter(id => id !== itemId)
        });
      } else
        this.setState({
          favorites: [...this.state.favorites, itemId]
        });
    };
    this.onSearch = textInput => {
      if (textInput === '') {
        this.setState({
          filteredItems: null
        });
      } else {
        let filteredGroups = this.appLauncherItems
          .map(group => {
            let filteredGroup = React.cloneElement(group, {
              children: group.props.children.filter(item => {
                if (item.type === ApplicationLauncherSeparator) return item;
                return item.props.children.toLowerCase().includes(textInput.toLowerCase());
              })
            });
            if (
              filteredGroup.props.children.length > 0 &&
              filteredGroup.props.children[0].type !== ApplicationLauncherSeparator
            )
              return filteredGroup;
          })
          .filter(newGroup => newGroup);

        if (filteredGroups.length > 0) {
          let lastGroup = filteredGroups.pop();
          lastGroup = React.cloneElement(lastGroup, {
            children: lastGroup.props.children.filter(item => item.type !== ApplicationLauncherSeparator)
          });
          filteredGroups.push(lastGroup);
        }

        this.setState({
          filteredItems: filteredGroups
        });
      }
    };
  }

  render() {
    const { isOpen, favorites, filteredItems } = this.state;
    return (
      <ApplicationLauncher
        onToggle={this.onToggle}
        onFavorite={this.onFavorite}
        onSearch={this.onSearch}
        isOpen={isOpen}
        items={filteredItems || this.appLauncherItems}
        favorites={favorites}
        isGrouped
      />
    );
  }
}
```

```js title=With-custom-icon
import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';

class ApplicationLauncheIcon extends React.Component {
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
    const appLauncherItems = [
      <ApplicationLauncherItem key="application_1a" href="#">
        Application 1 (anchor link)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="application_2a" component="button" onClick={() => alert('Clicked item 2')}>
        Application 2 (button with onClick)
      </ApplicationLauncherItem>,
      <ApplicationLauncherItem key="disabled_application_4a" isDisabled>
        Unavailable Application
      </ApplicationLauncherItem>
    ];
    return (
      <ApplicationLauncher
        onSelect={this.onSelect}
        onToggle={this.onToggle}
        isOpen={isOpen}
        items={appLauncherItems}
        toggleIcon={<HelpIcon />}
      />
    );
  }
}
```
