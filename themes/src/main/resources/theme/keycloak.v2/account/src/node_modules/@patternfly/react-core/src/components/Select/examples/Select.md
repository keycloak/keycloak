---
id: Select
section: components
cssPrefix: pf-c-select
propComponents: ['Select', 'SelectOption', 'SelectGroup', 'SelectOptionObject', 'SelectViewMoreObject']
ouia: true
---

import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';

## Examples

### Single

```js
import React from 'react';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import { Select, SelectOption, SelectVariant, SelectDirection, Checkbox, Divider } from '@patternfly/react-core';

class SingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      <SelectOption key={0} value="Select a title" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <Divider component="li" key={5} />,
      <SelectOption key={6} value="Dr" />,
      <SelectOption key={7} value="Other" />
    ];

    this.state = {
      isToggleIcon: false,
      isOpen: false,
      selected: null,
      isDisabled: false,
      direction: SelectDirection.down
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
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
    const { isOpen, selected, isDisabled, direction, isToggleIcon } = this.state;
    const titleId = 'title-id-1';
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
          isOpen={isOpen}
          aria-labelledby={titleId}
          isDisabled={isDisabled}
          direction={direction}
        >
          {this.options}
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

### Single with description

```js
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class SingleSelectDescription extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      { value: 'Mr', disabled: false },
      { value: 'Miss', disabled: false },
      { value: 'Mrs', disabled: false },
      { value: 'Ms', disabled: false },
      { value: 'Dr', disabled: false },
      { value: 'Other', disabled: false }
    ];

    this.state = {
      isOpen: false,
      selected: null,
      isDisabled: false
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, selected, isDisabled, direction, isToggleIcon } = this.state;
    const titleId = 'select-descriptions-title';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          variant={SelectVariant.single}
          placeholderText="Select an option"
          aria-label="Select Input with descriptions"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          isDisabled={isDisabled}
        >
          {this.options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              isPlaceholder={option.isPlaceholder}
              description="This is a description"
            />
          ))}
        </Select>
      </div>
    );
  }
}
```

### Grouped single

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, SelectGroup, Divider } from '@patternfly/react-core';

class GroupedSingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      selected: null
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection) => {
      this.setState({
        selected: selection,
        isOpen: false
      });
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
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <Divider key="divider" />,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];
  }

  render() {
    const { isOpen, selected } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status/vendor"
          aria-labelledby={titleId}
          isGrouped
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Validated

```js
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class ValidatedSelect extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      <SelectOption key={0} value="Select a title" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <SelectOption key={5} value="Dr" />,
      <SelectOption key={6} value="Other" />
    ];

    this.state = {
      isOpen: false,
      selected: null,
      isDisabled: false,
      validated: 'default'
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      let validatedState = 'success';
      if (isPlaceholder) {
        this.clearSelection();
        validatedState = 'error';
      } else {
        if (selection === 'Other') {
          validatedState = 'warning';
        } else {
          validatedState = 'success';
        }
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
      this.setState({
        validated: validatedState
      });
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, selected, isDisabled, direction, isToggleIcon, validated } = this.state;
    const titleId = 'select-validated-title';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          variant={SelectVariant.single}
          placeholderText="Select an option"
          aria-label="Select Input with validation"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          isDisabled={isDisabled}
          validated={validated}
          aria-describedby="validated-helper"
          aria-invalid={validated === 'error' ? true : false}
        >
          {this.options}
        </Select>
        <div aria-live="polite" id="validated-helper" hidden>
          {validated}
        </div>
      </div>
    );
  }
}
```

### Checkbox input

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, Divider } from '@patternfly/react-core';

class CheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
      <SelectOption key={0} value="Active" description="This is a description" />,
      <SelectOption key={1} value="Cancelled" />,
      <SelectOption key={2} value="Paused" />,
      <Divider key={3} />,
      <SelectOption key={4} value="Warning" />,
      <SelectOption key={5} value="Restarted" />
    ];
  }

  render() {
    const { isOpen, selected } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Checkbox input with counts

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, Divider } from '@patternfly/react-core';

class CheckboxSelectWithCounts extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
      <SelectOption key={0} value="Active" description="This is a description" itemCount={3} />,
      <SelectOption key={1} value="Cancelled" itemCount={1} />,
      <SelectOption key={2} value="Paused" itemCount={15} />,
      <SelectOption key={3} value="Warning" itemCount={2} />,
      <SelectOption key={4} value="Restarted" itemCount={8} />
    ];
  }

  render() {
    const { isOpen, selected } = this.state;
    const titleId = 'checkbox-select-with-counts-id';
    return (
      <div>
        <span id={titleId} hidden>
          Checkbox With Counts Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Select Input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Checkbox input no badge

```js
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class CheckboxSelectInputNoBadge extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
      <SelectOption key={3} value="Error" />
    ];
  }

  render() {
    const { isOpen, selected } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Grouped checkbox input

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, SelectGroup } from '@patternfly/react-core';

class GroupedCheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];
  }

  render() {
    const { isOpen, selected } = this.state;
    const titleId = 'grouped-checkbox-select-id-1';
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          isGrouped
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Grouped single with filtering

```js
import React from 'react';
import { Select, SelectOption, SelectGroup, SelectVariant, Checkbox } from '@patternfly/react-core';

class FilteringSingleSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: '',
      isCreatable: false,
      isInputValuePersisted: false,
      isInputFilterPersisted: false
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];

    this.onToggle = isOpen => {
      this.setState({ isOpen });
    };

    this.onSelect = (event, selection) => {
      this.setState({ selected: selection, isOpen: false }), console.log('selected: ', selection);
    };

    this.onFilter = (_, textInput) => {
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
          .filter(Boolean);
        return filteredGroups;
      }
    };

    this.toggleCreatable = checked => {
      this.setState({
        isCreatable: checked
      });
    };

    this.toggleInputValuePersisted = checked => {
      this.setState({
        isInputValuePersisted: checked
      });
    };

    this.toggleInputFilterPersisted = checked => {
      this.setState({
        isInputFilterPersisted: checked
      });
    };
  }

  render() {
    const {
      isOpen,
      selected,
      filteredOptions,
      isInputValuePersisted,
      isInputFilterPersisted,
      isCreatable
    } = this.state;
    const titleId = 'single-filtering-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Single select with filter
        </span>
        <Select
          variant={SelectVariant.single}
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          onFilter={this.onFilter}
          isGrouped
          hasInlineFilter
          isCreatable={isCreatable}
          isInputValuePersisted={isInputValuePersisted}
          isInputFilterPersisted={isInputFilterPersisted}
        >
          {this.options}
        </Select>
        <Checkbox
          label="isInputValuePersisted"
          isChecked={isInputValuePersisted}
          onChange={this.toggleInputValuePersisted}
          aria-label="toggle input value persisted"
          id="toggle-inline-filter-input-value-persisted"
          name="toggle-inline-filter-input-value-persisted"
        />
        <Checkbox
          label="isInputFilterPersisted"
          isChecked={isInputFilterPersisted}
          onChange={this.toggleInputFilterPersisted}
          aria-label="toggle input filter persisted"
          id="toggle-inline-filter-input-filter-persisted"
          name="toggle-inline-filter-input-filter-persisted"
        />
        <Checkbox
          label="isCreatable"
          isChecked={this.state.isCreatable}
          onChange={this.toggleCreatable}
          aria-label="toggle creatable checkbox"
          id="toggle-inline-filter-creatable-typeahead"
          name="toggle-inline-filter-creatable-typeahead"
        />
      </div>
    );
  }
}
```

### Grouped checkbox input with filtering

```js
import React from 'react';
import { Select, SelectOption, SelectGroup, SelectVariant } from '@patternfly/react-core';

class FilteringCheckboxSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: []
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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

    this.onFilter = (_, textInput) => {
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
    const { isOpen, selected, filteredOptions } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
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

### Grouped checkbox input with filtering and placeholder text

```js
import React from 'react';
import { Select, SelectOption, SelectGroup, SelectVariant } from '@patternfly/react-core';

class FilteringCheckboxSelectInputWithPlaceholder extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: []
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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

    this.onFilter = (_, textInput) => {
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
    const { isOpen, selected, filteredOptions } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          onFilter={this.onFilter}
          onClear={this.clearSelection}
          isGrouped
          hasInlineFilter
          inlineFilterPlaceholderText="Filter by status"
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Grouped checkbox input with filtering and custom badging

```js
import React from 'react';
import { Select, SelectOption, SelectGroup, SelectVariant } from '@patternfly/react-core';

class FilteringCheckboxSelectInputWithBadging extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: [],
      customBadgeText: 0
    };

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption key={0} value="Running" />
        <SelectOption key={1} value="Stopped" />
        <SelectOption key={2} value="Down" />
        <SelectOption key={3} value="Degraded" />
        <SelectOption key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption key={5} value="Dell" />
        <SelectOption key={6} value="Samsung" isDisabled />
        <SelectOption key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection) => {
      const { selected } = this.state;
      if (selected.includes(selection)) {
        this.setState(
          prevState => ({
            selected: prevState.selected.filter(item => item !== selection),
            customBadgeText: this.setBadgeText(prevState.selected.length - 1)
          }),
          () => console.log('selections: ', this.state.selected)
        );
      } else {
        this.setState(
          prevState => ({
            selected: [...prevState.selected, selection],
            customBadgeText: this.setBadgeText(prevState.selected.length + 1)
          }),
          () => console.log('selections: ', this.state.selected)
        );
      }
    };

    this.onFilter = (_, textInput) => {
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
        selected: [],
        customBadgeText: this.setBadgeText(0)
      });
    };

    this.setBadgeText = selected => {
      if (selected === 7) {
        return 'All';
      }
      if (selected === 0) {
        return 0;
      }
      return null;
    };
  }

  render() {
    const { isOpen, selected, filteredOptions, customBadgeText } = this.state;
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
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          onFilter={this.onFilter}
          onClear={this.clearSelection}
          isGrouped
          hasInlineFilter
          customBadgeText={customBadgeText}
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Typeahead

```js
import React from 'react';
import { Checkbox, Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class TypeaheadSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.defaultOptions = [
      { value: 'Alabama' },
      { value: 'Florida', description: 'This is a description' },
      { value: 'New Jersey' },
      { value: 'New Mexico' },
      { value: 'New York' },
      { value: 'North Carolina' }
    ];

    this.state = {
      options: this.defaultOptions,
      isOpen: false,
      selected: null,
      isDisabled: false,
      isCreatable: false,
      isInputValuePersisted: false,
      isInputFilterPersisted: false,
      hasOnCreateOption: false,
      resetOnSelect: true
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: this.state.resetOnSelect ? false : this.state.isOpen
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
        isOpen: false,
        options: this.defaultOptions
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

    this.toggleInputValuePersisted = checked => {
      this.setState({
        isInputValuePersisted: checked
      });
    };

    this.toggleInputFilterPersisted = checked => {
      this.setState({
        isInputFilterPersisted: checked
      });
    };

    this.toggleResetOnSelect = checked => {
      this.setState({
        resetOnSelect: checked
      });
    };
  }

  render() {
    const {
      isOpen,
      selected,
      isDisabled,
      isCreatable,
      hasOnCreateOption,
      isInputValuePersisted,
      isInputFilterPersisted,
      resetOnSelect,
      options
    } = this.state;
    const titleId = 'typeahead-select-id-1';
    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeahead}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          isInputValuePersisted={isInputValuePersisted}
          isInputFilterPersisted={isInputFilterPersisted}
          placeholderText="Select a state"
          isDisabled={isDisabled}
          isCreatable={isCreatable}
          onCreateOption={(hasOnCreateOption && this.onCreateOption) || undefined}
          shouldResetOnSelect={resetOnSelect}
        >
          {options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              {...(option.description && { description: option.description })}
            />
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
        <Checkbox
          label="isInputValuePersisted"
          isChecked={isInputValuePersisted}
          onChange={this.toggleInputValuePersisted}
          aria-label="toggle input value persisted"
          id="toggle-input-value-persisted"
          name="toggle-input-value-persisted"
        />
        <Checkbox
          label="isInputFilterPersisted"
          isChecked={isInputFilterPersisted}
          onChange={this.toggleInputFilterPersisted}
          aria-label="toggle input filter persisted"
          id="toggle-input-filter-persisted"
          name="toggle-input-filter-persisted"
        />
        <Checkbox
          label="shouldResetOnSelect"
          isChecked={this.state.resetOnSelect}
          onChange={this.toggleResetOnSelect}
          aria-label="toggle reset checkbox"
          id="toggle-reset-typeahead"
          name="toggle-reset-typeahead"
        />
      </div>
    );
  }
}
```

### Grouped typeahead

```js
import React from 'react';
import { Checkbox, Select, SelectGroup, SelectOption, SelectVariant, Divider } from '@patternfly/react-core';

class GroupedTypeaheadSelectInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      options: [
        <SelectGroup label="Status" key="group1">
          <SelectOption key={0} value="Running" />
          <SelectOption key={1} value="Stopped" />
          <SelectOption key={2} value="Down" />
          <SelectOption key={3} value="Degraded" />
          <SelectOption key={4} value="Needs maintenance" />
        </SelectGroup>,
        <Divider key="divider" />,
        <SelectGroup label="Vendor names" key="group2">
          <SelectOption key={5} value="Dell" />
          <SelectOption key={6} value="Samsung" isDisabled />
          <SelectOption key={7} value="Hewlett-Packard" />
        </SelectGroup>
      ],
      newOptions: [],
      isOpen: false,
      selected: null,
      isCreatable: false,
      hasOnCreateOption: false
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.onCreateOption = newValue => {
      this.setState({
        newOptions: [...this.state.newOptions, <SelectOption key={newValue} value={newValue} />]
      });
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
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
    const { isOpen, selected, isDisabled, isCreatable, hasOnCreateOption, options, newOptions } = this.state;
    const titleId = 'grouped-typeahead-select-id';
    const allOptions =
      newOptions.length > 0
        ? options.concat(
            <SelectGroup label="Created" key="create-group">
              {newOptions}
            </SelectGroup>
          )
        : options;
    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeahead}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
          isGrouped
          isCreatable={isCreatable}
          onCreateOption={(hasOnCreateOption && this.onCreateOption) || undefined}
        >
          {allOptions}
        </Select>
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

### Custom filtering

```js
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
      isOpen: false,
      selected: null
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };

    this.customFilter = (_, value) => {
      if (!value) {
        return this.options;
      }

      const input = new RegExp(value, 'i');
      return this.options.filter(child => input.test(child.props.value));
    };
  }

  render() {
    const { isOpen, selected } = this.state;
    const titleId = 'typeahead-select-id-2';
    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeahead}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          onFilter={this.customFilter}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Multiple

```js
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
        { value: 'New Mexico', disabled: false, description: 'This is a description' },
        { value: 'New York', disabled: false },
        { value: 'North Carolina', disabled: false }
      ],
      isOpen: false,
      selected: [],
      isCreatable: false,
      hasOnCreateOption: false,
      hasDisabledOption: false,
      resetOnSelect: true
    };

    this.onCreateOption = newValue => {
      this.setState({
        options: [...this.state.options, { value: newValue }]
      });
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        isOpen: false
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

    this.toggleOptionDisabled = toggleIndex => () => {
      this.setState(prevState => ({
        hasDisabledOption: !prevState.hasDisabledOption,
        options: prevState.options.map((option, index) =>
          index === toggleIndex ? { ...option, disabled: !option.disabled } : option
        )
      }));
    };

    this.toggleResetOnSelect = checked => {
      this.setState({
        resetOnSelect: checked
      });
    };
  }

  render() {
    const { isOpen, selected, isCreatable, hasOnCreateOption, resetOnSelect, options } = this.state;
    const titleId = 'multi-typeahead-select-id-1';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
          isCreatable={isCreatable}
          onCreateOption={(hasOnCreateOption && this.onCreateOption) || undefined}
          shouldResetOnSelect={resetOnSelect}
        >
          {options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              {...(option.description && { description: option.description })}
            />
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
        <Checkbox
          label="isDisabled (1st option only)"
          isChecked={this.state.hasDisabledOption}
          onChange={this.toggleOptionDisabled(0)}
          aria-label="toggle disable first option"
          id="toggle-disable-first-option"
          name="toggle-disable-first-option"
        />
        <Checkbox
          label="shouldResetOnSelect"
          isChecked={this.state.resetOnSelect}
          onChange={this.toggleResetOnSelect}
          aria-label="toggle multi reset checkbox"
          id="toggle-reset-multi-typeahead"
          name="toggle-reset-multi-typeahead"
        />
      </div>
    );
  }
}
```

### Multiple with Custom Chip Group Props

```js
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class MultiTypeaheadSelectInputWithChipGroupProps extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      options: [
        { value: 'Alabama', disabled: false },
        { value: 'Florida', disabled: false },
        { value: 'New Jersey', disabled: false },
        { value: 'New Mexico', disabled: false, description: 'This is a description' },
        { value: 'New York', disabled: false },
        { value: 'North Carolina', disabled: false }
      ],
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, selected, isCreatable, hasOnCreateOption } = this.state;
    const titleId = 'multi-typeahead-custom-chip-group-props-id-1';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          chipGroupProps={{ numChips: 1, expandedText: 'Hide', collapsedText: 'Show ${remaining}' }}
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
        >
          {this.state.options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              {...(option.description && { description: option.description })}
            />
          ))}
        </Select>
      </div>
    );
  }
}
```

### Multiple with Render Custom Chip Group

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, ChipGroup, Chip } from '@patternfly/react-core';

class MultiTypeaheadSelectInputWithChipGroupProps extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      options: [
        { value: 'Alabama', disabled: false },
        { value: 'Florida', disabled: false },
        { value: 'New Jersey', disabled: false },
        { value: 'New Mexico', disabled: false, description: 'This is a description' },
        { value: 'New York', disabled: false },
        { value: 'North Carolina', disabled: false }
      ],
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        isOpen: false
      });
    };
    this.chipGroupComponent = () => {
      const { selected } = this.state;
      return (
        <ChipGroup>
          {(selected || []).map((currentChip, index) => (
            <Chip
              isReadOnly={index === 0 ? true : false}
              key={currentChip}
              onClick={event => this.onSelect(event, currentChip)}
            >
              {currentChip}
            </Chip>
          ))}
        </ChipGroup>
      );
    };
  }

  render() {
    const { isOpen, selected, isCreatable, hasOnCreateOption } = this.state;
    const titleId = 'multi-typeahead-custom-chip-group-props-id-1';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          chipGroupProps={{ numChips: 1, expandedText: 'Hide', collapsedText: 'Show ${remaining}' }}
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
          chipGroupComponent={this.chipGroupComponent()}
        >
          {this.state.options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              {...(option.description && { description: option.description })}
            />
          ))}
        </Select>
      </div>
    );
  }
}
```

### Multiple with custom objects

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, Divider } from '@patternfly/react-core';

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
            .includes(value.toString().toLowerCase());
        }
      };
    };
    this.options = [
      <SelectOption key={0} value={this.createState('Alabama', 'AL', 'Montgomery', 1846)} />,
      <Divider component="li" key={111} />,
      <SelectOption key={1} value={this.createState('Florida', 'FL', 'Tailahassee', 1845)} />,
      <SelectOption key={2} value={this.createState('New Jersey', 'NJ', 'Trenton', 1787)} />,
      <SelectOption key={3} value={this.createState('New Mexico', 'NM', 'Santa Fe', 1912)} />,
      <SelectOption key={4} value={this.createState('New York', 'NY', 'Albany', 1788)} />,
      <SelectOption key={5} value={this.createState('North Carolina', 'NC', 'Raleigh', 1789)} />
    ];

    this.state = {
      isOpen: false,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, selected } = this.state;
    const titleId = 'multi-typeahead-select-id-2';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          onFilter={this.customFilter}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          placeholderText="Select a state"
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Plain multiple typeahead

```js
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
      isOpen: false,
      isPlain: true,
      selected: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, isPlain, selected } = this.state;
    const titleId = 'plain-typeahead-select-id';

    return (
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>
        <Select
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel="Select a state"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isOpen}
          isPlain={isPlain}
          aria-labelledby={titleId}
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

### Panel

```js
import React from 'react';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import { Select, SelectOption, SelectDirection, Checkbox } from '@patternfly/react-core';

class SingleSelectInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      isDisabled: false,
      direction: SelectDirection.down
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
    const { isOpen, selected, isDisabled, direction } = this.state;
    const titleId = 'title-id-2';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          aria-label="Select Input"
          onToggle={this.onToggle}
          isOpen={isOpen}
          aria-labelledby={titleId}
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

### Appending document body vs parent

Avoid passing in `document.body` when passing a value to the `menuAppendTo` prop on the Select component, as it can cause accessibility issues. These issues can include, but are not limited to, being unable to enter the contents of the Select options via assistive technologies (like keyboards or screen readers).

Instead append to `"parent"` to achieve the same result without sacrificing accessibility.

In this example, while, when the dropdown is opened, both Select variants handle focus management within their dropdown contents the same way, you'll notice a difference when you try pressing the Tab key after selecting an option.

For the `document.body` variant, the focus will be placed at the end of the page, since that is where the dropdown content is appended to in the DOM (rather than focus being placed on the second Select variant as one might expect). For the `"parent"` variant, however, the focus will be placed on the next tab-able element (the "Toggle JS code" button for the code editor in this case).

```js
import React from 'react';
import { Select, SelectOption, Flex, FlexItem } from '@patternfly/react-core';

class SelectDocumentBodyVsParent extends React.Component {
  constructor(props) {
    super(props);
    this.bodyOptions = [
      <SelectOption key={0} value="Select a title - document body" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <SelectOption key={6} value="Dr" />,
      <SelectOption key={7} value="Other" />
    ];

    this.parentOptions = [
      <SelectOption key={0} value="Select a title - parent" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <SelectOption key={6} value="Dr" />,
      <SelectOption key={7} value="Other" />
    ];

    this.state = {
      isBodyOpen: false,
      isParentOpen: false,
      bodySelected: null,
      parentSelected: null
    };

    this.onBodyToggle = isBodyOpen => {
      this.setState({
        isBodyOpen
      });
    };

    this.onParentToggle = isParentOpen => {
      this.setState({
        isParentOpen
      });
    };

    this.onBodySelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          bodySelected: selection,
          isBodyOpen: false
        });
        console.log('selected on document body:', selection);
      }
    };

    this.onParentSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          parentSelected: selection,
          isParentOpen: false
        });
        console.log('selected on parent:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };
  }

  render() {
    const { isBodyOpen, isParentOpen, bodySelected, parentSelected } = this.state;

    return (
      <Flex space={{ default: 'spacerMd' }} direction={{ default: 'column' }}>
        <FlexItem>
          <Select
            aria-label="Select Input for Document Body"
            onToggle={this.onBodyToggle}
            onSelect={this.onBodySelect}
            selections={bodySelected}
            isOpen={isBodyOpen}
            menuAppendTo={() => document.body}
          >
            {this.bodyOptions}
          </Select>
        </FlexItem>
        <FlexItem>
          <Select
            aria-label="Select Input for Parent"
            onToggle={this.onParentToggle}
            onSelect={this.onParentSelect}
            selections={parentSelected}
            isOpen={isParentOpen}
            menuAppendTo="parent"
          >
            {this.parentOptions}
          </Select>
        </FlexItem>
      </Flex>
    );
  }
}
```

### Favorites

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, SelectGroup } from '@patternfly/react-core';

class FavoritesSelect extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      selected: null,
      favorites: []
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
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

    this.options = [
      <SelectGroup label="Status" key="group1">
        <SelectOption id={'option-1'} key={0} value="Running" description="This is a description." />
        <SelectOption id={'option-2'} key={1} value="Stopped" />
        <SelectOption id={'option-3'} key={2} value="Down (disabled)" isDisabled />
        <SelectOption id={'option-4'} key={3} value="Degraded" />
        <SelectOption id={'option-5'} key={4} value="Needs maintenance" />
      </SelectGroup>,
      <SelectGroup label="Vendor names" key="group2">
        <SelectOption id={'option-6'} key={5} value="Dell" />
        <SelectOption id={'option-7'} key={6} value="Samsung" description="This is a description." />
        <SelectOption id={'option-8'} key={7} value="Hewlett-Packard" />
      </SelectGroup>
    ];
  }

  render() {
    const { isOpen, selected, favorites } = this.state;
    const titleId = 'grouped-single-select-id';
    return (
      <Select
        variant={SelectVariant.typeahead}
        typeAheadAriaLabel="Select value"
        onToggle={this.onToggle}
        onSelect={this.onSelect}
        selections={selected}
        isOpen={isOpen}
        placeholderText="Favorites"
        aria-labelledby={titleId}
        isGrouped
        onFavorite={this.onFavorite}
        favorites={favorites}
        onClear={this.clearSelection}
      >
        {this.options}
      </Select>
    );
  }
}
```

### Footer

```js
import React from 'react';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import { Select, SelectOption, SelectVariant, SelectDirection, Divider, Button } from '@patternfly/react-core';

class SelectWithFooter extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      <SelectOption key={0} value="Select a title" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <Divider component="li" key={5} />,
      <SelectOption key={6} value="Dr" />,
      <SelectOption key={7} value="Other" />
    ];

    this.state = {
      isToggleIcon: false,
      isOpen: false,
      selected: null,
      isDisabled: false,
      direction: SelectDirection.down
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };
  }

  render() {
    const { isOpen, selected, isDisabled, direction, isToggleIcon } = this.state;
    const titleId = 'title-id-footer';
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
          isOpen={isOpen}
          aria-labelledby={titleId}
          isDisabled={isDisabled}
          direction={direction}
          footer={
            <>
              <Button variant="link" isInline>
                Action
              </Button>
            </>
          }
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### Footer with checkboxes

```js
import React from 'react';
import { Select, SelectOption, SelectVariant, Button } from '@patternfly/react-core';

class SelectWithFooterCheckbox extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      selected: [],
      numOptions: 3,
      isLoading: false
    };

    this.options = [
      <SelectOption key={0} value="Active" description="This is a description" />,
      <SelectOption key={1} value="Cancelled" />,
      <SelectOption key={2} value="Paused" />,
      <SelectOption key={4} value="Warning" />,
      <SelectOption key={5} value="Restarted" />
    ];

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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
  }

  render() {
    const { isOpen, selected, isDisabled, direction, isToggleIcon } = this.state;
    const titleId = 'title-id-footer-checkbox';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Select input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          footer={
            <Button variant="link" isInline>
              Action
            </Button>
          }
        >
          {this.options}
        </Select>
      </div>
    );
  }
}
```

### View more

```js isBeta
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class SelectViewMore extends React.Component {
  constructor(props) {
    super(props);
    this.options = [
      <SelectOption key={0} value="Select a title" isPlaceholder />,
      <SelectOption key={1} value="Mr" />,
      <SelectOption key={2} value="Miss" />,
      <SelectOption key={3} value="Mrs" />,
      <SelectOption key={4} value="Ms" />,
      <SelectOption key={5} value="Dr" />,
      <SelectOption key={6} value="Other" />
    ];

    this.state = {
      isOpen: false,
      selected: null,
      numOptions: 3,
      isLoading: false
    };

    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };

    this.onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) this.clearSelection();
      else {
        this.setState({
          selected: selection,
          isOpen: false
        });
        console.log('selected:', selection);
      }
    };

    this.clearSelection = () => {
      this.setState({
        selected: null,
        isOpen: false
      });
    };

    this.simulateNetworkCall = callback => {
      setTimeout(callback, 2000);
    };

    this.onViewMoreClick = () => {
      // Set select loadingVariant to spinner then simulate network call before loading more options
      this.setState({ isLoading: true });
      this.simulateNetworkCall(() => {
        const newLength =
          this.state.numOptions + 3 <= this.options.length ? this.state.numOptions + 3 : this.options.length;
        this.setState({ numOptions: newLength, isLoading: false });
      });
    };
  }

  render() {
    const { isOpen, selected, isToggleIcon, numOptions, loadingVariant, isLoading } = this.state;
    const titleId = 'title-id-view-more';
    return (
      <div>
        <span id={titleId} hidden>
          Title
        </span>
        <Select
          variant={SelectVariant.single}
          aria-label="Select Input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          aria-labelledby={titleId}
          {...(!isLoading &&
            numOptions < this.options.length && {
              loadingVariant: { text: 'View more', onClick: this.onViewMoreClick }
            })}
          {...(isLoading && { loadingVariant: 'spinner' })}
        >
          {this.options.slice(0, numOptions)}
        </Select>
      </div>
    );
  }
}
```

### View more with checkboxes

```js isBeta
import React from 'react';
import { Select, SelectOption, SelectVariant } from '@patternfly/react-core';

class SelectViewMoreCheckbox extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
      selected: [],
      numOptions: 3,
      isLoading: false
    };

    this.options = [
      <SelectOption key={0} value="Active" description="This is a description" />,
      <SelectOption key={1} value="Cancelled" />,
      <SelectOption key={2} value="Paused" />,
      <SelectOption key={4} value="Warning" />,
      <SelectOption key={5} value="Restarted" />,
      <SelectOption key={6} value="Down" />,
      <SelectOption key={7} value="Disabled" />,
      <SelectOption key={8} value="Needs maintenance " />,
      <SelectOption key={9} value="Degraded " />
    ];

    this.onToggle = isOpen => {
      this.setState({
        isOpen
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

    this.simulateNetworkCall = callback => {
      setTimeout(callback, 2000);
    };

    this.onViewMoreClick = () => {
      // Set select loadingVariant to spinner then simulate network call before loading more options
      this.setState({ isLoading: true });
      this.simulateNetworkCall(() => {
        const newLength =
          this.state.numOptions + 3 <= this.options.length ? this.state.numOptions + 3 : this.options.length;
        this.setState({ numOptions: newLength, isLoading: false });
      });
    };
  }

  render() {
    const { isOpen, selected, numOptions, isLoading } = this.state;
    const titleId = 'view-more-checkbox-select-id';
    return (
      <div>
        <span id={titleId} hidden>
          Checkbox View more check
        </span>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Select input"
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          selections={selected}
          isOpen={isOpen}
          placeholderText="Filter by status"
          aria-labelledby={titleId}
          {...(!isLoading &&
            numOptions < this.options.length && {
              loadingVariant: { text: 'View more', onClick: this.onViewMoreClick }
            })}
          {...(isLoading && { loadingVariant: 'spinner' })}
        >
          {this.options.slice(0, numOptions)}
        </Select>
      </div>
    );
  }
}
```

### With a style applied to the placeholder text

```js
import React from 'react';
import { Select, SelectOption } from '@patternfly/react-core';

function SelectWithPlaceholderStyle() {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selected, setSelected] = React.useState([]);

  const options = [
    <SelectOption key={0} value="Active" />,
    <SelectOption key={1} value="Cancelled" />,
    <SelectOption key={2} value="Paused" />
  ];

  const onToggle = isOpen => setIsOpen(isOpen);

  const onSelect = (event, selection, isPlaceholder) => {
    setSelected(selection);
    setIsOpen(false);
  };

  const clearSelection = () => {
    setSelected(null);
    setIsOpen(false);
  };

  const titleId = 'placeholder-style-select-id';

  return (
    <div>
      <span id={titleId} hidden>
        Placeholder styles
      </span>
      <Select
        variant={SelectVariant.single}
        hasPlaceholderStyle
        aria-label="Select input"
        onToggle={onToggle}
        onSelect={onSelect}
        onClear={clearSelection}
        selections={selected}
        isOpen={isOpen}
        placeholderText="Filter by status"
        aria-labelledby={titleId}
      >
        {options}
      </Select>
    </div>
  );
}
```

### With a style applied to the placeholder option

```js
import React from 'react';
import { Select, SelectOption } from '@patternfly/react-core';

function SelectWithPlaceholderStyle() {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selected, setSelected] = React.useState([]);

  const options = [
    <SelectOption key={0} value="Filter by status" isPlaceholder />,
    <SelectOption key={1} value="Active" />,
    <SelectOption key={2} value="Cancelled" />,
    <SelectOption key={3} value="Paused" />
  ];

  const onToggle = isOpen => setIsOpen(isOpen);

  const onSelect = (event, selection, isPlaceholder) => {
    setSelected(selection);
    setIsOpen(false);
  };

  const clearSelection = () => {
    setSelected(null);
    setIsOpen(false);
  };

  const titleId = 'placeholder-style-select-option-id';

  return (
    <div>
      <span id={titleId} hidden>
        Placeholder styles - select option
      </span>
      <Select
        variant={SelectVariant.single}
        hasPlaceholderStyle
        aria-label="Select input"
        onToggle={onToggle}
        onSelect={onSelect}
        onClear={clearSelection}
        selections={selected}
        isOpen={isOpen}
        aria-labelledby={titleId}
      >
        {options}
      </Select>
    </div>
  );
}
```
