---
title: 'Input group'
section: components
cssPrefix: null
propComponents: ['InputGroup', 'InputGroupText']
typescript: true
---
import { DollarSignIcon, AtIcon, CalendarAltIcon, SearchIcon, QuestionCircleIcon } from '@patternfly/react-icons';
import {
  Button,
  ButtonVariant,
  TextArea,
  InputGroup,
  InputGroupText,
  TextInput,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Popover,
  PopoverPosition
} from '@patternfly/react-core';

## Examples
```js title=Basic
import React from 'react';
import { AtIcon, SearchIcon } from '@patternfly/react-icons';
import {
  Button,
  InputGroup,
  InputGroupText,
  TextInput
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <TextInput id="textInput6" type="email" aria-label="email input field" />
          <InputGroupText id="email-example">@example.com</InputGroupText>
        </InputGroup>
        <br />
        <InputGroup>
          <InputGroupText id="username" aria-label="@">
            <AtIcon />
          </InputGroupText>
          <TextInput isValid={false} id="textInput7" type="email" aria-label="Error state username example" />
        </InputGroup>
        <br />
        <InputGroup>
          <TextInput name="textInput11" id="textInput11" type="search" aria-label="search input example" />
          <Button variant="control" aria-label="search button for search input">
            <SearchIcon />
          </Button>
        </InputGroup>
      </React.Fragment>
    );
  }
}
```

```js title=With-textarea
import React from 'react';
import {
  Button,
  TextArea,
  InputGroup
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <TextArea name="textarea2" id="textarea2" aria-label="textarea with button" />
          <Button id="textAreaButton2" variant="control">
            Button
          </Button>
        </InputGroup>
      </React.Fragment>
    );
  }
}
```

```js title=With-dropdown
import React from 'react';
import {
  Button,
  InputGroup,
  TextInput,
  Dropdown,
  DropdownToggle,
  DropdownItem
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      selected: ''
    };
    this.onToggle = isOpen => {
      this.setState({
        isOpen
      });
    };
    this.onSelect = event => {
      this.setState({
        isOpen: false,
        selected: event.currentTarget.value
      });
    };
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <Dropdown
            onSelect={this.onSelect}
            toggle={
              <DropdownToggle onToggle={this.onToggle}>
                {this.state.selected ? this.state.selected : 'Dropdown'}
              </DropdownToggle>
            }
            isOpen={this.state.isOpen}
            dropdownItems={[
              <DropdownItem key="opt-1" value="Option 1" component="button">
                Option 1
              </DropdownItem>,
              <DropdownItem key="opt-2" value="Option 2" component="button">
                Option 2
              </DropdownItem>,
              <DropdownItem key="opt-3" value="Option 3" component="button">
                Option 3
              </DropdownItem>
            ]}
          />
          <TextInput id="textInput3" aria-label="input with dropdown and button" />
          <Button id="inputDropdownButton1" variant="control">Button</Button>
        </InputGroup>
      </React.Fragment>
    );
  }
}
```

```js title=With-datepicker
import React from 'react';
import { CalendarAltIcon } from '@patternfly/react-icons';
import {
  InputGroup,
  InputGroupText,
  TextInput
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <InputGroupText component="label" htmlFor="textInput9">
            <CalendarAltIcon />
          </InputGroupText>
          <TextInput name="textInput9" id="textInput9" type="date" aria-label="Date input example" />
        </InputGroup>
      </React.Fragment>
    );
  }
}
```

```js title=With-popover
import React from 'react';
import { QuestionCircleIcon } from '@patternfly/react-icons';
import {
  Button,
  InputGroup,
  TextInput,
  Popover,
  PopoverPosition
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <TextInput name="textInput10" id="textInput10" type="search" aria-label="input example with popover" />
          <Popover
            aria-label="popover example"
            position={PopoverPosition.top}
            bodyContent="This field is an example of input group with popover"
          >
            <Button variant="control" aria-label="popover for input">
              <QuestionCircleIcon />
            </Button>
          </Popover>
        </InputGroup>
      </React.Fragment>
    );
  }
}
```


```js title=With-multiple-group-siblings
import React from 'react';
import { DollarSignIcon } from '@patternfly/react-icons';
import {
  Button,
  TextArea,
  InputGroup,
  InputGroupText,
  TextInput
} from '@patternfly/react-core';

class SimpleInputGroups extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <InputGroup>
          <Button id="textAreaButton1" variant="control">
            Button
          </Button>
          <TextArea name="textarea1" id="textarea1" aria-label="textarea with buttons" />
          <Button variant="control">Button</Button>
        </InputGroup>
        <br />
        <InputGroup>
          <Button id="textAreaButton3" variant="control">
            Button
          </Button>
          <Button variant="control">Button</Button>
          <TextArea name="textarea3" id="textarea3" aria-label="textarea with 3 buttons" />
          <Button variant="control">Button</Button>
        </InputGroup>
        <br />
        <InputGroup>
          <InputGroupText>
            <DollarSignIcon />
          </InputGroupText>
          <TextInput id="textInput5" type="number" aria-label="Dollar amount input example" />
          <InputGroupText>.00</InputGroupText>
        </InputGroup>
      </React.Fragment>
    );
  }
}
```
