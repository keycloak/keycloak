---
id: 'Search input'
section: components
cssPrefix: 'pf-c-search-input'
propComponents: ['SearchInput', 'SearchAttribute']
beta: true
---

import { SearchInput } from '@patternfly/react-core';
import { ExternalLinkSquareAltIcon } from '@patternfly/react-icons';

## Examples

### Basic

```js
import React from 'react';
import { SearchInput } from '@patternfly/react-core';

class BasicSearchInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };

    this.onChange = (value, event) => {
      this.setState({
        value: value
      });
    };
  }

  render() {
    return (
      <SearchInput
        placeholder="Find by name"
        value={this.state.value}
        onChange={this.onChange}
        onClear={evt => this.onChange('', evt)}
      />
    );
  }
}
```

### Match with result count

```js
import React from 'react';
import { SearchInput } from '@patternfly/react-core';

class SearchInputWithResultCount extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: '',
      resultsCount: 0
    };

    this.onChange = (value, event) => {
      this.setState({
        value: value,
        resultsCount: 3
      });
    };

    this.onClear = event => {
      this.setState({
        value: '',
        resultsCount: 0
      });
    };
  }

  render() {
    return (
      <SearchInput
        placeholder="Find by name"
        value={this.state.value}
        onChange={this.onChange}
        onClear={this.onClear}
        resultsCount={this.state.resultsCount}
      />
    );
  }
}
```

### Match with navigable options

```js
import React from 'react';
import { SearchInput } from '@patternfly/react-core';

class SearchInputWithNavigableOptions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: '',
      resultsCount: 0,
      currentResult: 1,
      isPreviousNavigationButtonDisabled: false,
      isNextNavigationButtonDisabled: false
    };

    this.onChange = (value, event) => {
      this.setState({
        value: value,
        resultsCount: 3
      });
    };

    this.onClear = event => {
      this.setState({
        value: '',
        resultsCount: 0,
        currentResult: 1
      });
    };

    this.onNext = event => {
      this.setState(prevState => {
        const newCurrentResult = prevState.currentResult + 1;
        return {
          currentResult: newCurrentResult <= prevState.resultsCount ? newCurrentResult : prevState.resultsCount
        };
      });
    };

    this.onPrevious = event => {
      this.setState(prevState => {
        const newCurrentResult = prevState.currentResult - 1;
        return {
          currentResult: newCurrentResult > 0 ? newCurrentResult : 1
        };
      });
    };
  }
  render() {
    return (
      <SearchInput
        placeholder="Find by name"
        value={this.state.value}
        onChange={this.onChange}
        onClear={this.onClear}
        isNextNavigationButtonDisabled={this.state.currentResult === 3}
        isPreviousNavigationButtonDisabled={this.state.currentResult === 1}
        resultsCount={`${this.state.currentResult} / ${this.state.resultsCount}`}
        onNextClick={this.onNext}
        onPreviousClick={this.onPrevious}
      />
    );
  }
}
```

### With submit button

```js
import React from 'react';
import { SearchInput } from '@patternfly/react-core';

SubmitButtonSearchInput = () => {
  const [value, setValue] = React.useState('');

  return (
    <SearchInput
      placeholder='Find by name'
      value={value}
      onChange={setValue}
      onSearch={setValue}
      onClear={() => setValue('')}
    />
  );
}

```

### Focus search input using ref

```js
import React from 'react';
import { SearchInput, Button } from '@patternfly/react-core';

TextInputSelectAll = () => {
  const [value, setValue] = React.useState('');
  const ref = React.useRef(null);
  return (
    <React.Fragment>
      <SearchInput ref={ref} value={value} onChange={setValue} onClear={() => setValue('')} />
      <Button onClick={() => ref.current && ref.current.focus()}>Focus on the search input</Button>
    </React.Fragment>
  );
};
```

### Advanced

The search input component can be used to dynamically build a one to one attribute-value advanced search.
Using the `attributes` prop alongside the `advancedSearchDelimiter` will expose this functionality, as demonstrated in
the following example. The search input component can also be used as a composable component and paired with a Popper 
or other elements to build a completely custom advanced search form. This feature is demonstrated 
in the search input's <a href="/components/search-input/react-demos">react demos</a>.

```js
import React from 'react';
import { Button, Checkbox, FormGroup, SearchInput } from '@patternfly/react-core';
import ExternalLinkSquareAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-square-alt-icon';

AdvancedSearchInput = () => {
  const [value, setValue] = React.useState('username:player firstname:john');
  const [useEqualsAsDelimiter, setUseEqualsAsDelimiter] = React.useState(false);
  const [useCustomFooter, setUseCustomFooter] = React.useState(false);

  const toggleDelimiter = checked => {
    const newValue = value.replace(/:|=/g, checked ? '=' : ':');
    setUseEqualsAsDelimiter(checked);
    setValue(newValue);
  };

  return (
    <>
      <Checkbox
        label="Use equal sign as search attribute delimiter"
        isChecked={useEqualsAsDelimiter}
        onChange={toggleDelimiter}
        aria-label="change delimiter checkbox"
        id="toggle-delimiter"
        name="toggle-delimiter"
      />
      <Checkbox
        label="Add custom footer element after the attributes in the menu"
        isChecked={useCustomFooter}
        onChange={value => setUseCustomFooter(value)}
        aria-label="change use custom footer checkbox"
        id="toggle-custom-footer"
        name="toggle-custom-footer"
      />
      <br />
      <SearchInput
        attributes={[
          { attr: 'username', display: 'Username' },
          { attr: 'firstname', display: 'First name' }
        ]}
        advancedSearchDelimiter={useEqualsAsDelimiter ? '=' : ':'}
        value={value}
        onChange={setValue}
        onSearch={setValue}
        onClear={() => setValue('')}
        formAdditionalItems={
          useCustomFooter ? (
            <FormGroup>
              <Button variant="link" isInline icon={<ExternalLinkSquareAltIcon />} iconPosition="right">
                Link
              </Button>
            </FormGroup>
          ) : null
        }
      />
    </>
  );
};
```
