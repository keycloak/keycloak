---
id: Masthead
section: components
---

import imgBrand from '@patternfly/react-core/src/demos/examples/pfColorLogo.svg';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';

## Demos

### Basic

```ts
import React from 'react';
import {
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  Button,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
  ContextSelector,
  ContextSelectorItem,
  Dropdown,
  DropdownSeparator,
  DropdownToggle,
  KebabToggle,
  DropdownItem,
  Brand
} from '@patternfly/react-core';
import imgBrand from '@patternfly/react-core/src/demos/examples/pfColorLogo.svg';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';

class BasicMasthead extends React.Component {
  items = [
    'My Project',
    'OpenShift Cluster',
    'Production Ansible',
    'AWS',
    'Azure',
    'My Project 2',
    'OpenShift Cluster',
    'Production Ansible 2',
    'AWS 2',
    'Azure 2'
  ];

  state = {
    isOpen: false,
    isDropdownOpen: false,
    isKebabOpen: false,
    selected: this.items[0],
    searchValue: '',
    filteredItems: this.items
  };

  onToggle = (event: any, isOpen: boolean) => {
    this.setState({
      isOpen
    });
  };

  onDropdownToggle = (isDropdownOpen: boolean) => {
    this.setState({
      isDropdownOpen
    });
  };

  onKebabToggle = (isKebabOpen: boolean) => {
    this.setState({
      isKebabOpen
    });
  };

  onSelect = (event: any, value: string) => {
    this.setState({
      selected: value,
      isOpen: !this.state.isOpen
    });
  };

  onDropdownSelect = () => {
    this.setState({
      isDropdownOpen: !this.state.isDropdownOpen
    });
  };

  onKebabSelect = () => {
    this.setState({
      isKebabOpen: !this.state.isKebabOpen
    });
  };

  onSearchInputChange = (value: string) => {
    this.setState({ searchValue: value });
  };

  onSearchButtonClick = () => {
    const filtered =
      this.state.searchValue === ''
        ? this.items
        : this.items.filter(str => str.toLowerCase().indexOf(this.state.searchValue.toLowerCase()) !== -1);

    this.setState({ filteredItems: filtered || [] });
  };

  render() {
    const { isOpen, isDropdownOpen, isKebabOpen, selected, searchValue, filteredItems } = this.state;

    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled href="www.google.com">
        Disabled Link
      </DropdownItem>,
      <DropdownItem
        key="disabled action"
        isAriaDisabled
        component="button"
        tooltip="Tooltip for disabled item"
        tooltipProps={{ position: 'top' }}
      >
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];

    return (
      <Masthead id="basic">
        <MastheadToggle>
          <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
            <BarsIcon />
          </Button>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand>
            <Brand src={imgBrand} alt="Patternfly logo" />
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>
          <Toolbar id="toolbar" isFullHeight isStatic>
            <ToolbarContent>
              <ToolbarItem>
                <ContextSelector
                  toggleText={selected}
                  onSearchInputChange={this.onSearchInputChange}
                  isOpen={isOpen}
                  searchInputValue={searchValue}
                  onToggle={this.onToggle}
                  onSelect={this.onSelect}
                  onSearchButtonClick={this.onSearchButtonClick}
                  screenReaderLabel="Selected Project:"
                  isFullHeight
                >
                  {filteredItems.map((item, index) => (
                    <ContextSelectorItem key={index}>{item}</ContextSelectorItem>
                  ))}
                </ContextSelector>
              </ToolbarItem>
              <ToolbarGroup alignment={{ default: 'alignRight' }}>
                <ToolbarItem visibility={{ default: 'hidden', lg: 'visible' }}>
                  <Dropdown
                    onSelect={this.onDropdownSelect}
                    toggle={
                      <DropdownToggle id="toggle-id" onToggle={this.onDropdownToggle} toggleIndicator={CaretDownIcon}>
                        Dropdown
                      </DropdownToggle>
                    }
                    isOpen={isDropdownOpen}
                    dropdownItems={dropdownItems}
                    isFullHeight
                  />
                </ToolbarItem>
                <ToolbarItem>
                  <Dropdown
                    onSelect={this.onKebabSelect}
                    position="right"
                    toggle={<KebabToggle onToggle={this.onKebabToggle} id="toggle-id-kebab" />}
                    isOpen={isKebabOpen}
                    isPlain
                    dropdownItems={dropdownItems}
                  />
                </ToolbarItem>
              </ToolbarGroup>
            </ToolbarContent>
          </Toolbar>
        </MastheadContent>
      </Masthead>
    );
  }
}
```
