---
title: 'Select'
section: components
cssPrefix: 'pf-c-select'
propComponents: ['Select', 'SelectOption', 'SelectGroup', 'SelectOptionObject']
typescript: true
---

import { Select, SelectOption, SelectVariant, SelectGroup, SelectDirection, Checkbox } from '@patternfly/react-core';

## Examples

```js title=Single
import React from 'react';
import { CubeIcon } from '@patternfly/react-icons';
import { Select, SelectOption, SelectVariant, Checkbox } from '@patternfly/react-core';

class SingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      { value: 'Choose...', disabled: false, isPlaceholder: true },
      { value: 'Mr', disabled: false },
      { value: 'Miss', disabled: false },
      { value: 'Mrs', disabled: false },
      { value: 'Ms', disabled: false },
      { value: 'Dr', disabled: false },
      { value: 'Other', disabled: false }
    ];

    this.state = {
      isToggleIcon: false,
      isExpanded: false,
      selected: null,
      isDisabled: false,
      direction: SelectDirection.down
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isExpanded: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isExpanded: false
      });
    };

    this.toggleDisabled = checked => {
      this.setState({
        isDisabled: checked
      });
    };

    this.setIcon = checked => {
      this.setState({
        isToggleIcon: checked
      });
    };

    this.toggleDirection = () => {
      if (this.state.direction === SelectDirection.up) {
        this.setState({
          direction: SelectDirection.down
        });
      } else {
        this.setState({
          direction: SelectDirection.up
        });
      }
    };
  }

  render() {
    const { isExpanded, selected, isDisabled, direction, isToggleIcon } = this.state;
    const titleId = 'title-id';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          toggleIcon={isToggleIcon && <CubeIcon />}
          variant={SelectVariant.single}
          aria-label="Select Input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          isDisabled={isDisabled}
          direction={direction}
        >
          {this.options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              isPlaceholder={option.isPlaceholder}
            />
          ))}
        </Select>
        <Checkbox
          label="isDisabled"
          isChecked={this.state.isDisabled}
          onChange={this.toggleDisabled}
          aria-label="disabled checkbox"
          id="toggle-disabled"
          name="toggle-disabled"
        />
        <Checkbox
          label="Expands up"
          isChecked={direction === SelectDirection.up}
          onChange={this.toggleDirection}
          aria-label="direction checkbox"
          id="toggle-direction"
          name="toggle-direction"
        />
        <Checkbox
          label="Show icon"
          isChecked={isToggleIcon}
          onChange={this.setIcon}
          aria-label="show icon checkbox"
          id="toggle-icon"
          name="toggle-icon"
        />
      </div>
    );
  }
}
```

```js title=Grouped-single
import React from 'react';
import { Select, SelectOption, SelectVariant, SelectGroup } from '@patternfly/react-core';

class GroupedSingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isExpanded: false,
      selected: null
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      this.setState({
        selected: selection,
        isExpanded: false
      })
    };

    this.clearSelection = () => {
      this.setState({
        selected: null
      });
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs Maintenence" />
      </SelectGroup>,
      <SelectGroup label="Vendor Names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];
  }

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'grouped-single-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Grouped Checkbox Title
        </span>
        <Select
          variant={SelectVariant.single}
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isExpanded={isExpanded}
          placeholderText="Filter by status/vendor"
          ariaLabelledBy={titleId}
          isGrouped
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Checkbox-input
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class CheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isExpanded: false,
      selected: []
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: []
      });
    };

    this.options = [
      <SelectOption key={0} value="Active" />,
      <SelectOption key={1} value="Cancelled" />,
      <SelectOption key={2} value="Paused" />,
      <SelectOption key={3} value="Warning" />,
      <SelectOption key={4} value="Restarted" />
    ];
  }

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'checkbox-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Checkbox Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Select Input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isExpanded={isExpanded}
          placeholderText="Filter by status"
          ariaLabelledBy={titleId}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Checkbox-input-no-badge
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class CheckboxSelectInputNoBadge extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isExpanded: false,
      selected: []
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: []
      });
    };

    this.options = [
      <SelectOption key={0} value="Debug" />,
      <SelectOption key={1} value="Info" />,
      <SelectOption key={2} value="Warn" />,
      <SelectOption key={3} value="Error" />,
    ];
  }

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'checkbox-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Checkbox Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Select Input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isCheckboxSelectionBadgeHidden
          isExpanded={isExpanded}
          placeholderText="Filter by status"
          ariaLabelledBy={titleId}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Grouped-checkbox-input
import React from 'react';
import { Select, SelectOption, SelectVariant, SelectGroup } from '@patternfly/react-core';

class GroupedCheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isExpanded: false,
      selected: []
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: []
      });
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs Maintenence" />
      </SelectGroup>,
      <SelectGroup label="Vendor Names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];
  }

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'grouped-checkbox-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Grouped Checkbox Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isExpanded={isExpanded}
          placeholderText="Filter by status"
          ariaLabelledBy={titleId}
          isGrouped
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Grouped-checkbox-input-with-filtering
import React from 'react';
import { Select, SelectOption, SelectGroup, SelectVariant } from '@patternfly/react-core';

class FilteringCheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isExpanded: false,
      selected: []
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs Maintenence" />
      </SelectGroup>,
      <SelectGroup label="Vendor Names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.onFilter = evt => {
      const textInput = evt.target.value;
      if (textInput === '') {
        return this.options;
      } else {
        let filteredGroups = this.options
          .map(group => {
            let filteredGroup = React.cloneElement(group, {
              children: group.props.children.filter(item => {
                return item.props.value.toLowerCase().includes(textInput.toLowerCase());
              })
            });
            if (filteredGroup.props.children.length > 0) return filteredGroup;
          })
          .filter(newGroup => newGroup);
        return filteredGroups;
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: []
      });
    };
  }

  render() {
    const { isExpanded, selected, filteredOptions } = this.state;
    const titleId = 'checkbox-filtering-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Checkbox Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isExpanded={isExpanded}
          placeholderText="Filter by status"
          ariaLabelledBy={titleId}
          onFilter={this.onFilter}
          onClear={this.clearSelection}
          isGrouped
          hasInlineFilter
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Typeahead
import React from 'react';
import { Checkbox, Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class TypeaheadSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      options: [
        { value: 'Alabama' },
        { value: 'Florida' },
        { value: 'New Jersey' },
        { value: 'New Mexico' },
        { value: 'New York' },
        { value: 'North Carolina' }
      ],
      isExpanded: false,
      selected: null,
      isDisabled: false,
      isCreatable: false,
      hasOnCreateOption: false
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isExpanded: false
        });
        console.log('selected:', selection);
      }
    };

    this.onCreateOption = newValue => {
      this.setState({
        options: [...this.state.options, { value: newValue }]
      });
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isExpanded: false
      });
    };

    this.toggleDisabled = checked => {
      this.setState({
        isDisabled: checked
      });
    };

    this.toggleCreatable = checked => {
      this.setState({
        isCreatable: checked
      });
    };

    this.toggleCreateNew = checked => {
      this.setState({
        hasOnCreateOption: checked
      });
    };
  }

  render() {
    const { isExpanded, selected, isDisabled, isCreatable, hasOnCreateOption, options } = this.state;
    const titleId = 'typeahead-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeahead}
          ariaLabelTypeAhead="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          placeholderText="Select a state"
          isDisabled={isDisabled}
          isCreatable={isCreatable}
          onCreateOption={(hasOnCreateOption && this.onCreateOption) || undefined}
        >
          {options.map((option, index) => (
            <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
          ))}
        </Select>
        <Checkbox
          label="isDisabled"
          isChecked={this.state.isDisabled}
          onChange={this.toggleDisabled}
          aria-label="toggle disabled checkbox"
          id="toggle-disabled-typeahead"
          name="toggle-disabled-typeahead"
        />
        <Checkbox
          label="isCreatable"
          isChecked={this.state.isCreatable}
          onChange={this.toggleCreatable}
          aria-label="toggle creatable checkbox"
          id="toggle-creatable-typeahead"
          name="toggle-creatable-typeahead"
        />
        <Checkbox
          label="onCreateOption"
          isChecked={this.state.hasOnCreateOption}
          onChange={this.toggleCreateNew}
          aria-label="toggle new checkbox"
          id="toggle-new-typeahead"
          name="toggle-new-typeahead"
        />
      </div>
    );
  }
}
```

```js title=Custom-filtering
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class TypeaheadSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      <SelectOption key={0} value="Alabama" />,
      <SelectOption key={1} value="Florida" />,
      <SelectOption key={2} value="New Jersey" />,
      <SelectOption key={3} value="New Mexico" />,
      <SelectOption key={4} value="New York" />,
      <SelectOption key={5} value="North Carolina" />
    ];
    this.state = {
      isExpanded: false,
      selected: null,
      options: this.options
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded,
        options: this.options
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isExpanded: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isExpanded: false,
        options: this.options
      });
    };

    this.customFilter = e => {
      let input;
      try {
        input = new RegExp(e.target.value, 'i');
      } catch (err) {}
      let typeaheadFilteredChildren =
        e.target.value !== '' ? this.options.filter(child => input.test(child.props.value)) : this.options;
      this.setState({
        options: typeaheadFilteredChildren
      });
    };
  }

  render() {
    const { isExpanded, selected, options } = this.state;
    const titleId = 'typeahead-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeahead}
          ariaLabelTypeAhead="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          onFilter={this.customFilter}
          selections={selected}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          placeholderText="Select a state"
        >
          {options}
        </Select>
      </div>
    );
  }
}
```

```js title=Multiple
import React from 'react';
import { Checkbox, Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class MultiTypeaheadSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      options: [
        { value: 'Alabama', disabled: false },
        { value: 'Florida', disabled: false },
        { value: 'New Jersey', disabled: false },
        { value: 'New Mexico', disabled: false },
        { value: 'New York', disabled: false },
        { value: 'North Carolina', disabled: false }
      ],
      isExpanded: false,
      selected: [],
      isCreatable: false,
      hasOnCreateOption: false
    };

    this.onCreateOption = newValue => {
      this.setState({
        options: [...this.state.options, { value: newValue }]
      });
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: [],
        isExpanded: false
      });
    };

    this.toggleCreatable = checked => {
      this.setState({
        isCreatable: checked
      });
    };

    this.toggleCreateNew = checked => {
      this.setState({
        hasOnCreateOption: checked
      });
    };
  }

  render() {
    const { isExpanded, selected, isCreatable, hasOnCreateOption } = this.state;
    const titleId = 'multi-typeahead-select-id';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          ariaLabelTypeAhead="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          placeholderText="Select a state"
          isCreatable={isCreatable}
          onCreateOption={(hasOnCreateOption && this.onCreateOption) || undefined}
        >
          {this.state.options.map((option, index) => (
            <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
          ))}
        </Select>
        <Checkbox
          label="isCreatable"
          isChecked={this.state.isCreatable}
          onChange={this.toggleCreatable}
          aria-label="toggle creatable checkbox"
          id="toggle-creatable-typeahead-multi"
          name="toggle-creatable-typeahead-multi"
        />
        <Checkbox
          label="onCreateOption"
          isChecked={this.state.hasOnCreateOption}
          onChange={this.toggleCreateNew}
          aria-label="toggle new checkbox"
          id="toggle-new-typeahead-multi"
          name="toggle-new-typeahead-multi"
        />
      </div>
    );
  }
}
```

```js title=Multiple-with-custom-objects
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class MultiTypeaheadSelectInputCustomObjects extends React.Component {
  constructor(props) {
    super(props);
    this.createState = (name, abbreviation, capital, founded) => {
      return {
        name: name,
        abbreviation: abbreviation,
        capital: capital,
        founded: founded,
        toString: function() {
          return `${this.name} (${this.abbreviation}) - Founded: ${this.founded}`;
        },
        compareTo: function(value) {
          return this.toString()
            .toLowerCase()
            .includes(value.toLowerCase());
        }
      };
    };
    this.options = [
      <SelectOption key={0} value={this.createState('Alabama', 'AL', 'Montgomery', 1846)} />,
      <SelectOption key={1} value={this.createState('Florida', 'FL', 'Tailahassee', 1845)} />,
      <SelectOption key={2} value={this.createState('New Jersey', 'NJ', 'Trenton', 1787)} />,
      <SelectOption key={3} value={this.createState('New Mexico', 'NM', 'Santa Fe', 1912)} />,
      <SelectOption key={4} value={this.createState('New York', 'NY', 'Albany', 1788)} />,
      <SelectOption key={5} value={this.createState('North Carolina', 'NC', 'Raleigh', 1789)} />
    ];

    this.state = {
      isExpanded: false,
      selected: []
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: [],
        isExpanded: false
      });
    };

    this.customFilter = e => {
      console.log(e);
      const input = e.target.value.toString();
      let typeaheadFilteredChildren =
        input !== '' ? this.options.filter(option => option.props.value.compareTo(input)) : this.options;
      return typeaheadFilteredChildren;
    };
  }

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'multi-typeahead-select-id';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          ariaLabelTypeAhead="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          onFilter={this.customFilter}
          selections={selected}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          placeholderText="Select a state"
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

```js title=Plain-multiple-typeahead
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class PlainSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      { value: 'Alabama', disabled: false },
      { value: 'Florida', disabled: false },
      { value: 'New Jersey', disabled: false },
      { value: 'New Mexico', disabled: false },
      { value: 'New York', disabled: false },
      { value: 'North Carolina', disabled: false }
    ];

    this.state = {
      isExpanded: false,
      isPlain: true,
      selected: []
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({ selected: prevState.selected.filter(item => item !== selection) }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({ selected: [...prevState.selected, selection] }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: [],
        isExpanded: false
      });
    };
  }

  render() {
    const { isExpanded, isPlain, selected } = this.state;
    const titleId = 'plain-typeahead-select-id';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          ariaLabelTypeAhead="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isExpanded={isExpanded}
          isPlain={isPlain}
          ariaLabelledBy={titleId}
          placeholderText="Select a state"
        >
          {this.options.map((option, index) => (
            <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
          ))}
        </Select>
      </div>
    );
  }
}
```

```js title=Panel
import React from 'react';
import { CubeIcon } from '@patternfly/react-icons';
import { Select, SelectOption, SelectVariant, Checkbox } from '@patternfly/react-core';

class SingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isExpanded: false,
      isDisabled: false,
      direction: SelectDirection.down
    };

    this.onToggle = isExpanded => {
      this.setState({
        isExpanded
      });
    };

    this.toggleDisabled = checked => {
      this.setState({
        isDisabled: checked
      });
    };

    this.toggleDirection = () => {
      if (this.state.direction === SelectDirection.up) {
        this.setState({
          direction: SelectDirection.down
        });
      } else {
        this.setState({
          direction: SelectDirection.up
        });
      }
    };
  }

  render() {
    const { isExpanded, selected, isDisabled, direction } = this.state;
    const titleId = 'title-id';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          variant={SelectVariant.panel}
          aria-label="Select Input"
          onToggle={this.onToggle}
          isExpanded={isExpanded}
          ariaLabelledBy={titleId}
          isDisabled={isDisabled}
          direction={direction}
          customContent="[Panel contents here]"
          placeholderText="Filter by birth month"
        />
        <Checkbox
          label="isDisabled"
          isChecked={this.state.isDisabled}
          onChange={this.toggleDisabled}
          aria-label="disabled checkbox panel"
          id="toggle-disabled-panel"
          name="toggle-disabled-panel"
        />
        <Checkbox
          label="Expands up"
          isChecked={direction === SelectDirection.up}
          onChange={this.toggleDirection}
          aria-label="direction checkbox panel"
          id="toggle-direction-panel"
          name="toggle-direction-panel"
        />
      </div>
    );
  }
}
```
