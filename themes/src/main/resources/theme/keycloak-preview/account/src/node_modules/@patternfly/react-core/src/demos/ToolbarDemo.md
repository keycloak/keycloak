---
title: 'Toolbar'
section: 'demos'
---
import {
  Button,
  Dropdown,
  DropdownPosition,
  DropdownToggle,
  DropdownItem,
  KebabToggle,
  TextInput,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
  ToolbarSection
} from '@patternfly/react-core';
import { ListUlIcon, SortAlphaDownIcon, TableIcon } from '@patternfly/react-icons';

## Examples
```js title=Basic
import React from 'react';
import {
  Button,
  Dropdown,
  DropdownPosition,
  DropdownToggle,
  DropdownItem,
  KebabToggle,
  TextInput,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
  ToolbarSection
} from '@patternfly/react-core';
import { ListUlIcon, SortAlphaDownIcon, TableIcon } from '@patternfly/react-icons';

class ComplexToolbarDemo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropDownOpen: false,
      isKebabOpen: false,
      searchValue: ''
    };
    this.handleTextInputChange = value => {
      this.setState({ searchValue: value });
    };

    this.onDropDownToggle = isOpen => {
      this.setState({
        isDropDownOpen: isOpen
      });
    };

    this.onDropDownSelect = event => {
      this.setState({
        isDropDownOpen: !this.state.isDropDownOpen
      });
    };

    this.onKebabToggle = isOpen => {
      this.setState({
        isKebabOpen: isOpen
      });
    };

    this.onKebabSelect = event => {
      this.setState({
        isKebabOpen: !this.state.isKebabOpen
      });
    };

    this.buildSearchBox = () => {
      let { value } = this.state.searchValue;
      return (
        <TextInput value={value ? value : ''} type="search" onChange={this.handleTextInputChange} aria-label="search text input" />
      );
    };

    this.buildDropdown = () => {
      const { isDropDownOpen } = this.state;
      return (
        <Dropdown
            onSelect={this.onDropDownSelect}
            position={DropdownPosition.right}
            toggle={<DropdownToggle onToggle={this.onDropDownToggle}>All</DropdownToggle>}
            isOpen={isDropDownOpen}
            dropdownItems={[
            <DropdownItem key="item-1">Item 1</DropdownItem>,
            <DropdownItem key="item-2">Item 2</DropdownItem>,
            <DropdownItem key="item-3">Item 3</DropdownItem>,
            <DropdownItem isDisabled key="all">
                All
            </DropdownItem>
            ]}
        />
      );
    };

    this.buildKebab = () => {
      const { isKebabOpen } = this.state;

      return (
        <Dropdown
            onSelect={this.onKebabSelect}
            position={DropdownPosition.right}
            toggle={<KebabToggle onToggle={this.onKebabToggle} />}
            isOpen={isKebabOpen}
            isPlain
            dropdownItems={[
            <DropdownItem key="link">Link</DropdownItem>,
            <DropdownItem component="button" key="action_button">
                Action
            </DropdownItem>,
            <DropdownItem isDisabled key="disabled_link">
                Disabled Link
            </DropdownItem>,
            <DropdownItem isDisabled component="button" key="disabled_button">
                Disabled Action
            </DropdownItem>
            ]}
        />
      );
    };
  }

  render() {
    return (
      <Toolbar className="pf-l-toolbar pf-u-justify-content-space-between pf-u-mx-xl pf-u-my-md">
        <ToolbarGroup>
          <ToolbarItem className="pf-u-mr-xl">{this.buildSearchBox()}</ToolbarItem>
          <ToolbarItem className="pf-u-mr-md">{this.buildDropdown()}</ToolbarItem>
          <ToolbarItem>
            <Button variant="plain" aria-label="Sort A-Z">
              <SortAlphaDownIcon />
            </Button>
          </ToolbarItem>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarItem>
            <Button variant="plain" aria-label="Insert Table">
              <TableIcon />
            </Button>
          </ToolbarItem>
          <ToolbarItem className="pf-u-mx-md">
            <Button variant="plain" aria-label="Insert Bulleted List">
              <ListUlIcon />
            </Button>
          </ToolbarItem>
          <ToolbarItem>
            <Button variant="plain" aria-label="Action 1">
              Action
            </Button>
          </ToolbarItem>
          <ToolbarItem className="pf-u-mx-md">
            <Button aria-label="Action 2">Action</Button>
          </ToolbarItem>
          <ToolbarItem>{this.buildKebab()}</ToolbarItem>
        </ToolbarGroup>
        <ToolbarSection aria-label="Toolbar Section">
          <ToolbarGroup>
            <ToolbarItem>17 of 80 items</ToolbarItem>
          </ToolbarGroup>
        </ToolbarSection>
      </Toolbar>
    );
  }
}
```
