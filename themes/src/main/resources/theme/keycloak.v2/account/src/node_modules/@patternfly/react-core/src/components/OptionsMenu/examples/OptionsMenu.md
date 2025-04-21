---
id: Options menu
section: components
cssPrefix: pf-c-options-menu
propComponents: ['OptionsMenu', 'OptionsMenuItem', 'OptionsMenuSeparator', 'OptionsMenuToggle', 'OptionsMenuToggleWithText']
ouia: true
---
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import SortAmountDownIcon from '@patternfly/react-icons/dist/esm/icons/sort-amount-down-icon';

## Examples
### Single option
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggle } from '@patternfly/react-core';

class SingleOption extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleTemplateText: "Options menu",
        selectedOption: "singleOption1"
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
      
      this.onSelect = event => {
        const id = event.currentTarget.id;
        this.setState(() => {
          return { selectedOption: id };
        });
      };
      
    }
    
  render() {
    const { selectedOption, toggleTemplateText, isOpen } = this.state;
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption1"} id="singleOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption2"} id="singleOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption3"} id="singleOption3" key="option 3">Option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-single-option-example" 
        menuItems={menuItems} 
        isOpen={isOpen} 
        toggle={toggle}/>
    );
  }
}
```

### Disabled
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggle } from '@patternfly/react-core';

class DisabledOptionsMenu extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleTemplateText: "Disabled options menu"
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
    }
    
  render() {
    const { toggleTemplateText, isOpen } = this.state;
    const menuItems = [];
    const toggle = <OptionsMenuToggle isDisabled onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-single-disabled-example-toggle" 
        isOpen={isOpen} 
        menuItems={menuItems}
        toggle={toggle}/>
    );
  }
}
```

### Multiple options
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuSeparator, OptionsMenuItemGroup, OptionsMenuToggle} from '@patternfly/react-core';

class MultipleOptions extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleTemplateText: "Sort by",
        sortColumn: "date",
        sortDirection: "ascending"
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
      
      this.onSelectColumn = event => {
        const id = event.currentTarget.id;
        this.setState(() => {
          return { sortColumn: id };
        });
      };
      
      this.onSelectDirection = event => {
        const id = event.currentTarget.id;
        this.setState(() => {
          return { sortDirection: id };
        });
      };
    }

  render() {
    const { sortColumn, sortDirection, toggleTemplateText, isOpen } = this.state;
    const menuItems = [
        <OptionsMenuItemGroup key="first group" aria-label="Sort Column">
          <OptionsMenuItem onSelect={this.onSelectColumn} isSelected={sortColumn === "name"} id="name" key="name">Name</OptionsMenuItem>
          <OptionsMenuItem onSelect={this.onSelectColumn} isSelected={sortColumn === "date"} id="date" key="date">Date</OptionsMenuItem>
          <OptionsMenuItem isDisabled onSelect={this.onSelectColumn} isSelected={sortColumn === "disabled"} id="disabled" key="disabled">Disabled</OptionsMenuItem>
          <OptionsMenuItem onSelect={this.onSelectColumn} isSelected={sortColumn === "size"} id="size" key="size">Size</OptionsMenuItem>
        </OptionsMenuItemGroup>,
        <OptionsMenuSeparator key="separator"/>,
        <OptionsMenuItemGroup key="second group" aria-label="Sort Direction">
          <OptionsMenuItem onSelect={this.onSelectDirection} isSelected={sortDirection === "ascending"} id="ascending" key="ascending">Ascending</OptionsMenuItem>
          <OptionsMenuItem onSelect={this.onSelectDirection} isSelected={sortDirection === "descending"} id="descending" key="descending">Descending</OptionsMenuItem>
        </OptionsMenuItemGroup>
      ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-multiple-options-example" 
        menuItems={menuItems} 
        isOpen={isOpen}
        toggle={toggle}
        isGrouped />
    );
  }
}
```

### Plain
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggle } from '@patternfly/react-core';
import SortAmountDownIcon from '@patternfly/react-icons/dist/esm/icons/sort-amount-down-icon';

class Plain extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        isDisabledOpen: false,
        plainOption1: true,
        plainOption2: false,
        plainOption3: false,
        disabledPlainOption1: true,
        disabledPlainOption2: false,
        disabledPlainOption3: false
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };

      this.onDisabledToggle = () => {
          this.setState({
              isDisabledOpen: !this.state.isDisabledOpen
          });
      };
      
      this.onSelect = event => {
        const id = event.currentTarget.id;
        this.setState((prevState) => {
          return { [id]: !prevState[id] };
        });
      };
    }

  render() {
    const { isOpen, isDisabledOpen, plainOption1, plainOption2, plainOption3, disabledPlainOption1, disabledPlainOption2, disabledPlainOption3 } = this.state
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={plainOption1} id="plainOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={plainOption2} id="plainOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={plainOption3} id="plainOption3" key="option 3">Option 3</OptionsMenuItem>
    ];

    const disabledMenuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={disabledPlainOption1} id="disabledPlainOption1" key="disabled option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={disabledPlainOption2} id="disabledPlainOption2" key="disabled option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={disabledPlainOption3} id="disabledPlainOption3" key="disabled option 3">Option 3</OptionsMenuItem>
    ];
    const toggleTemplate = <SortAmountDownIcon aria-hidden="true"/>
    
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplate} aria-label="Sort by" hideCaret/>

    const disabledToggle = <OptionsMenuToggle isDisabled onToggle={this.onDisabledToggle} toggleTemplate={toggleTemplate} aria-label="Sort by" hideCaret/>

    return (
      <React.Fragment>
        <OptionsMenu id="options-menu-plain-disabled-example" 
          isPlain
          menuItems={disabledMenuItems}  
          isOpen={isDisabledOpen}
          toggle={disabledToggle}/>
        <OptionsMenu id="options-menu-plain-example" 
          isPlain
          menuItems={menuItems}  
          isOpen={isOpen}
          toggle={toggle}/>
      </React.Fragment>
    );
  }
}
```

### Align top
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuDirection, OptionsMenuToggle } from '@patternfly/react-core';

class Top extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      toggleTemplateText: "Options menu",
      topOption1: false,
      topOption2: false,
      topOption3: false
    };

    this.onToggle = () => {
        this.setState({
            isOpen: !this.state.isOpen
        });
    };
    
    this.onSelect = event => {
      const id = event.currentTarget.id;
      this.setState((prevState) => {
        return { [id]: !prevState[id] };
      });
    };
  }

  render() {
    const { isOpen, topOption1, topOption2, topOption3, toggleTemplateText } = this.state
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={topOption1} id="topOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={topOption2} id="topOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={topOption3} id="topOption3" key="option 3">Option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-top-example" 
        direction={OptionsMenuDirection.up} 
        menuItems={menuItems} 
        toggle={toggle} 
        isOpen={isOpen} />
    );
  }
}
```

### Align right
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuPosition, OptionsMenuToggle } from '@patternfly/react-core';

class AlignRight extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      toggleTemplateText: "Align right",
      rightOption1: true,
      rightOption2: false,
      rightOption3: false
    };

    this.onToggle = () => {
        this.setState({
            isOpen: !this.state.isOpen
        });
    };
    
    this.onSelect = event => {
      const id = event.currentTarget.id;
      this.setState((prevState) => {
        return { [id]: !prevState[id] };
      });
    };
  }

  render() {
    const { isOpen, toggleTemplateText, rightOption1, rightOption2, rightOption3 } = this.state;
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={rightOption1} id="rightOption1" key="option 1">Right option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={rightOption2} id="rightOption2" key="option 2">Right option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={rightOption3} id="rightOption3" key="option 3">Right option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-align-right-example" 
        position={OptionsMenuPosition.right} 
        menuItems={menuItems} 
        toggle={toggle} 
        isOpen={isOpen} />
    );
  }
}
```

### Plain with text
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggleWithText } from '@patternfly/react-core';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';

class PlainWithText extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleText: <React.Fragment>Custom text</React.Fragment>,
        buttonContents: <CaretDownIcon/>,
        customOption1: true,
        customOption2: false,
        customOption3: false
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
      
      this.onSelect = event => {
        const id = event.currentTarget.id;
        this.setState((prevState) => {
          return { [id]: !prevState[id] };
        });
      };

      this.onToggle = () => {
        this.setState({
          isOpen: !this.state.isOpen
        });
      };
    }

  render() {
    const { isOpen, toggleText, buttonContents } = this.state;
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption1} id="customOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption2} id="customOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption3} id="customOption3" key="option 3">Option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggleWithText toggleText={toggleText} toggleButtonContents={buttonContents} onToggle={this.onToggle} />;

    return (
      <OptionsMenu 
        id="options-menu-plain-with-text-example" 
        menuItems={menuItems} 
        isOpen={isOpen} 
        isPlain
        isText
        toggle={toggle} />
    );
  }
}
```

### Plain with text disabled
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggleWithText } from '@patternfly/react-core';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';

class PlainWithText extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleText: <React.Fragment>Custom text</React.Fragment>,
        buttonContents: <CaretDownIcon/>,
        customOption1: true,
        customOption2: false,
        customOption3: false
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
      
      this.onSelect = event => {
        const id = event.currentTarget.id;
        this.setState((prevState) => {
          return { [id]: !prevState[id] };
        });
      };

      this.onToggle = () => {
        this.setState({
          isOpen: !this.state.isOpen
        });
      };
    }

  render() {
    const { isOpen, toggleText, buttonContents } = this.state;
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption1} id="customOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption2} id="customOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={this.state.customOption3} id="customOption3" key="option 3">Option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggleWithText isDisabled toggleText={toggleText} toggleButtonContents={buttonContents} onToggle={this.onToggle} />;

    return (
      <OptionsMenu 
        id="options-menu-plain-with-text-example" 
        menuItems={menuItems} 
        isOpen={isOpen} 
        isPlain
        isText
        toggle={toggle} />
    );
  }
}
```

### Grouped items with titles
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggle, OptionsMenuItemGroup } from '@patternfly/react-core';

class GroupedItems extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      toggleTemplateText: "Options menu",
      selectedOption: "groupOption1"
    };
    
    this.onToggle = () => {
      this.setState({
        isOpen: !this.state.isOpen
      });
    };
    
    this.onSelect = event => {
      const id = event.currentTarget.id;
      this.setState(() => {
        return { selectedOption: id };
      });
    };
  }
  
  render() {
    const { isOpen, selectedOption, toggleTemplateText } = this.state;
    
    const menuGroups = [
      <OptionsMenuItemGroup hasSeparator key="group0">
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption1"} id="groupOption1" key="option 1">Option 1</OptionsMenuItem>
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption2"} id="groupOption2" key="option 2">Option 2</OptionsMenuItem>
      </OptionsMenuItemGroup>,
      <OptionsMenuItemGroup groupTitle="Group 1" hasSeparator key="group1">
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption3"} id="groupOption3" key="option 3">Option 1</OptionsMenuItem>
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption4"} id="groupOption4" key="option 4">Option 2</OptionsMenuItem>
      </OptionsMenuItemGroup>,
      <OptionsMenuItemGroup groupTitle="Group 2" key="group2">
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption5"} id="groupOption5" key="option 5">Option 1</OptionsMenuItem>
        <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "groupOption6"} id="groupOption6" key="option 6">Option 2</OptionsMenuItem>
      </OptionsMenuItemGroup>
    ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />
    
    return (
      <OptionsMenu 
        id="options-menu-align-right-example" 
        position={OptionsMenuPosition.right} 
        menuItems={menuGroups} 
        toggle={toggle} 
        isOpen={isOpen} 
        isGrouped />
    );
  }
}

```

### Single option with menu on document body
```js
import React from 'react';
import { OptionsMenu, OptionsMenuItem, OptionsMenuToggle } from '@patternfly/react-core';

class SingleOption extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
        isOpen: false,
        toggleTemplateText: "Options menu",
        selectedOption: "singleOption1"
      };

      this.onToggle = () => {
          this.setState({
              isOpen: !this.state.isOpen
          });
      };
      
      this.onSelect = event => {
        const id = event.currentTarget.id;
        this.setState(() => {
          return { selectedOption: id };
        });
      };
      
    }
    
  render() {
    const { selectedOption, toggleTemplateText, isOpen } = this.state;
    const menuItems = [
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption1"} id="singleOption1" key="option 1">Option 1</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption2"} id="singleOption2" key="option 2">Option 2</OptionsMenuItem>,
      <OptionsMenuItem onSelect={this.onSelect} isSelected={selectedOption === "singleOption3"} id="singleOption3" key="option 3">Option 3</OptionsMenuItem>
    ];
    const toggle = <OptionsMenuToggle onToggle={this.onToggle} toggleTemplate={toggleTemplateText} />

    return (
      <OptionsMenu 
        id="options-menu-single-option-example" 
        menuItems={menuItems} 
        isOpen={isOpen} 
        toggle={toggle}
        menuAppendTo={() => document.body}
      />
    );
  }
}
```
