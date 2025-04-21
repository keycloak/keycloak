---
id: Search input
section: components
beta: true
---
import { 
  Button, 
  Card, 
  CardBody, 
  CardFooter, 
  DatePicker,
  Form, 
  FormGroup,
  Grid,
  GridItem, 
  Menu,
  MenuContent,
  MenuItem,
  MenuList,
  MenuToggle,
  Popper, 
  SearchInput, 
  TextInput 
} from '@patternfly/react-core';
import { words } from './words.js';

## Demos

### Search with autocomplete

This demo handles building the advanced search form using the composable Menu, and the `SearchInput`'s `hint` prop.
It also demonstrates wiring up the appropriate keyboard interactions, focus management, and general event handling.


```js
import React from 'react';
import { 
  Menu,
  MenuContent,
  MenuItem,
  MenuList,
  Popper, 
  SearchInput
} from '@patternfly/react-core';

import { words } from './words.js';

SearchAutocomplete = () => {
  const [value, setValue] = React.useState('');
  const [hint, setHint] = React.useState('');
  const [autocompleteOptions, setAutocompleteOptions] = React.useState([]);
  
  const [isAutocompleteOpen, setIsAutocompleteOpen] = React.useState(false);
  
  const searchInputRef = React.useRef(null);
  const autocompleteRef = React.useRef(null);
  
  const onClear = () => {
    setValue('');
  };
  
  const onChange = (newValue) => {
    if (newValue !== '' && searchInputRef && searchInputRef.current && searchInputRef.current.contains(document.activeElement)) {
      setIsAutocompleteOpen(true);
      
      // When the value of the search input changes, build a list of no more than 10 autocomplete options.
      // Options which start with the search input value are listed first, followed by options which contain
      // the search input value.
      let options = words.filter((option) => option.startsWith(newValue.toLowerCase())).map((option) => <MenuItem itemId={option} key={option}>{option}</MenuItem>);
      if (options.length > 10) {
        options = options.slice(0,10);
      } else {
        options = [...options, ...words.filter((option) => !option.startsWith(newValue.toLowerCase()) && option.includes(newValue.toLowerCase())).map((option) => <MenuItem itemId={option} key={option}>{option}</MenuItem>)].slice(0, 10)
      }
      
      // The hint is set whenever there is only one autocomplete option left.
      setHint(options.length === 1? options[0].props.itemId : '');
      // The menu is hidden if there are no options
      setIsAutocompleteOpen(options.length > 0);
      setAutocompleteOptions(options);
    } else {
      setIsAutocompleteOpen(false);
    }
    setValue(newValue);
  };
  
  // Whenever an autocomplete option is selected, set the search input value, close the menu, and put the browser
  // focus back on the search input
  const onSelect = (e, itemId) => {
    e.stopPropagation();
    setValue(itemId);
    setIsAutocompleteOpen(false);
    searchInputRef.current.focus();
  };
  
  const handleMenuKeys = event => {
    // If there is a hint while the browser focus is on the search input, tab or right arrow will 'accept' the hint value 
    // and set it as the search input value
    if (hint && (event.key === 'Tab' || event.key === 'ArrowRight') && searchInputRef.current === event.target) {
      setValue(hint);
      setHint('');
      setIsAutocompleteOpen(false);
      if (event.key === 'ArrowRight') {
        event.preventDefault();   
      }
    // if the autocomplete is open and the browser focus is on the search input,
    } else if (isAutocompleteOpen && searchInputRef.current && searchInputRef.current === event.target) {
      // the escape key closes the autocomplete menu and keeps the focus on the search input.
      if (event.key === 'Escape') {
        setIsAutocompleteOpen(false);
        searchInputRef.current.focus();
      // the up and down arrow keys move browser focus into the autocomplete menu
      } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        const firstElement = autocompleteRef.current.querySelector('li > button:not(:disabled)');
        firstElement && firstElement.focus();
        event.preventDefault(); // by default, the up and down arrow keys scroll the window
      // the tab, enter, and space keys will close the menu, and the tab key will move browser
      // focus forward one element (by default)
      }  else if (event.key === 'Tab'|| event.key === "Enter" || event.key === 'Space'){
        setIsAutocompleteOpen(false);
        if (event.key === "Enter" || event.key === 'Space') {
          event.preventDefault();
        }
      }
    // If the autocomplete is open and the browser focus is in the autocomplete menu
    // hitting tab will close the autocomplete and but browser focus back on the search input.  
    } else if (isAutocompleteOpen && autocompleteRef.current.contains(event.target) && (event.key === 'Tab')) {
      event.preventDefault();
      setIsAutocompleteOpen(false);
      searchInputRef.current.focus();
    }
    
  };
  
  // The autocomplete menu should close if the user clicks outside the menu.
  const handleClickOutside = event => {
    if (isAutocompleteOpen && autocompleteRef && autocompleteRef.current && !autocompleteRef.current.contains(event.target)) {
      setIsAutocompleteOpen(false);
    }
  };
  
  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isAutocompleteOpen, hint, searchInputRef.current]);
  
  const searchInput = (
    <SearchInput
      value={value}
      onChange={onChange}
      onClear={onClear}
      ref={searchInputRef}
      hint={hint}
      id="autocomplete-search"
    />
  );
  
  const autocomplete = (
    <Menu ref={autocompleteRef} onSelect={onSelect}>
      <MenuContent>
        <MenuList>
          {autocompleteOptions}
        </MenuList>
      </MenuContent>
    </Menu>
  );
  

  return (
    <Popper
      trigger={searchInput}
      popper={autocomplete}
      isVisible={isAutocompleteOpen}
      enableFlip={false}
      // append the autocomplete menu to the search input in the DOM for the sake of the keyboard navigation experience
      appendTo={() => document.querySelector('#autocomplete-search')}
    />
  );
};
```

### Composable advanced search

This demo handles building the advanced search form using the composable Menu, as well as wiring up a 
select using the composable Menu and MenuToggle components. This demo also demonstrates wiring up the appropriate
keyboard interactions, focus management, and general event handling.

Note: This demo and its handling of 'date within' and a date picker is modeled after the gmail advanced search form.

```js
import React from 'react';
import { 
  ActionGroup,
  Button, 
  DatePicker,
  Form, 
  FormGroup,
  Grid,
  GridItem, 
  isValidDate,
  Menu,
  MenuContent,
  MenuItem,
  MenuList,
  MenuToggle,
  Panel,
  PanelMain,
  PanelMainBody,
  Popper, 
  SearchInput, 
  TextInput,
  yyyyMMddFormat
} from '@patternfly/react-core';

AdvancedComposableSearchInput = () => {
  const [value, setValue] = React.useState('');
  const [hasWords, setHasWords] = React.useState('');
  const [dateWithin, setDateWithin] = React.useState('1 day');
  const [date, setDate] = React.useState();
  
  const [isAdvancedSearchOpen, setIsAdvancedSearchOpen] = React.useState(false);
  const [isDateWithinOpen, setIsDateWithinOpen] = React.useState(false);
    
  const isInitialMount = React.useRef(true);
  const firstAttrRef = React.useRef(null);
  const searchInputRef = React.useRef(null);
  const advancedSearchPaneRef = React.useRef(null);
  const dateWithinToggleRef = React.useRef();
  const dateWithinMenuRef = React.useRef();
  
  const onClear = () => {
    setValue('');
    setHasWords('');
    setDateWithin('');
    setDate('');
  };
  
  const onChange = (value) => {
    if (value.length <= hasWords.length + 1) {
      setValue(value);
      setHasWords(value);
    } else {
      setValue(hasWords);
    }
  };
  
  // After initial page load, whenever the advanced search menu is opened, the browser focus should be placed on the
  // first advanced search form input. Whenever the advanced search menu is closed, the browser focus should
  // be returned to the search input.
  React.useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
    } else {
      if (isAdvancedSearchOpen && firstAttrRef && firstAttrRef.current) {
        firstAttrRef.current.focus();
      } else if (!isAdvancedSearchOpen && searchInputRef) {
        searchInputRef.current.focus();
      }
    }
  }, [isAdvancedSearchOpen]);
  
  // If a menu is open and has browser focus, then the escape key closes them and puts the browser focus onto their 
  // respective toggle. The 'date within' menu also needs to close when the 'tab' key is hit. However, hitting tab while
  // focus is in the advanced search form should move the focus to the next form input, not close the advanced search
  // menu.
  const handleMenuKeys = event => {
    if (isDateWithinOpen && dateWithinMenuRef.current && dateWithinMenuRef.current.contains(event.target)) {
      if (event.key === 'Escape' || event.key === 'Tab') {
        setIsDateWithinOpen(!isDateWithinOpen);
        dateWithinToggleRef.current.focus();
      }
    } 
    if (isAdvancedSearchOpen && advancedSearchPaneRef.current && advancedSearchPaneRef.current.contains(event.target)) {
      if (event.key === 'Escape' || 
      (event.key === 'Tab' && !event.shiftKey && advancedSearchPaneRef.current.querySelector('button[type=reset]') === event.target)
      ) {
        setIsAdvancedSearchOpen(!isAdvancedSearchOpen);
        searchInputRef.current.focus();
      }
    }  
  };
  
  // If a menu is open and has browser focus, then clicking outside the menu should close it.
  const handleClickOutside = event => {
    if (isDateWithinOpen && dateWithinMenuRef && dateWithinMenuRef.current && !dateWithinMenuRef.current.contains(event.target)) {
      setIsDateWithinOpen(false);
    } 
    if (isAdvancedSearchOpen && advancedSearchPaneRef && advancedSearchPaneRef.current && !advancedSearchPaneRef.current.contains(event.target)) {
      setIsAdvancedSearchOpen(false);
    }
  };
  
  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [dateWithinMenuRef.current, advancedSearchPaneRef.current, isAdvancedSearchOpen, isDateWithinOpen]);
  
  
  // This demo and its handling of 'date within' and a date picker is modeled after the gmail advanced search form.
  const onSubmit = (value, event) => {
    event.preventDefault();
    
    if (isValidDate(new Date(date)) && dateWithin) {
      let afterDate = new Date(date);
      let toDate = new Date(date);
      switch (dateWithin) {
        case '1 day':
          afterDate.setDate(afterDate.getDate());
          toDate.setDate(toDate.getDate() + 2);
          break;
        case '3 days':
          afterDate.setDate(afterDate.getDate() - 2);
          toDate.setDate(toDate.getDate() + 4);
          break;
        case '1 week':
          afterDate.setDate(afterDate.getDate() - 6);
          toDate.setDate(toDate.getDate() + 8);
          break;
        case '2 weeks':
          afterDate.setDate(afterDate.getDate() - 13);
          toDate.setDate(toDate.getDate() + 15);
          break;
        case '1 month':
          afterDate.setMonth(afterDate.getMonth() - 1);
          afterDate.setDate(afterDate.getDate() + 1);
          toDate.setMonth(toDate.getMonth() + 1);
          toDate.setDate(toDate.getDate() + 1);
          break;
        case '2 months':
          afterDate.setMonth(afterDate.getMonth() - 2);
          afterDate.setDate(afterDate.getDate() + 1);
          toDate.setMonth(toDate.getMonth() + 2);
          toDate.setDate(toDate.getDate() + 1);
          break;
        case '6 months':
          afterDate.setMonth(afterDate.getMonth() - 6);
          afterDate.setDate(afterDate.getDate() + 1);
          toDate.setMonth(toDate.getMonth() + 6);
          toDate.setDate(toDate.getDate() + 1);
          break;
        case '1 year':
          afterDate.setFullYear(afterDate.getFullYear() - 1);
          afterDate.setDate(afterDate.getDate() + 1);
          toDate.setFullYear(toDate.getFullYear() + 1);
          toDate.setDate(toDate.getDate() + 1);
          break;
      }
      setValue(`${hasWords && (hasWords + " ")}after:${yyyyMMddFormat(afterDate)} to:${yyyyMMddFormat(toDate)}`)
    } else {
      setValue(hasWords);
    }
    
    setIsAdvancedSearchOpen(false);
  };
  
  const searchInput = (
    <SearchInput
      value={value}
      onChange={onChange}
      onToggleAdvancedSearch={(e, isOpen) => {
        e.stopPropagation();
        setIsAdvancedSearchOpen(isOpen)
      }}
      isAdvancedSearchOpen={isAdvancedSearchOpen}
      onClear={onClear}
      onSearch={onSubmit}
      ref={searchInputRef}
      id="custom-advanced-search"
    />
  );
  
  // Clicking the 'date within' toggle should open its associated menu and then place the browser 
  // focus on the first menu item. 
  const toggleDateWithinMenu = ev => {
    ev.stopPropagation(); // Stop handleClickOutside from handling
    setTimeout(() => {
      if (dateWithinMenuRef.current) {
        const firstElement = dateWithinMenuRef.current.querySelector('li > button:not(:disabled)');
        firstElement && firstElement.focus();
      }
    }, 0);
    setIsDateWithinOpen(!isDateWithinOpen);
  };
  
  // Selecting a date within option closes the menu, sets the value of date within, and puts browser focus back
  // on the date within toggle.
  const onDateWithinSelect = (e, itemId) => {
    e.stopPropagation();
    setIsDateWithinOpen(false);
    setDateWithin(itemId);
    if (dateWithinToggleRef && dateWithinToggleRef.current) {
      dateWithinToggleRef.current.focus();
    }
  };
  
  const dateWithinOptions = (
    <Menu ref={dateWithinMenuRef} selected={dateWithin} onSelect={onDateWithinSelect}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId="1 day">1 day</MenuItem>
          <MenuItem itemId="3 days">3 days</MenuItem>
          <MenuItem itemId="1 week">1 week</MenuItem>
          <MenuItem itemId="2 weeks">2 weeks</MenuItem>
          <MenuItem itemId="1 month">1 month</MenuItem>
          <MenuItem itemId="2 months">2 months</MenuItem>
          <MenuItem itemId="6 months">6 months</MenuItem>
          <MenuItem itemId="1 year">1 year</MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );
  
  const dateWithinToggle = (
    <MenuToggle ref={dateWithinToggleRef} onClick={toggleDateWithinMenu} isExpanded={isDateWithinOpen} style={{width: "100%"}}>
      {dateWithin}
    </MenuToggle>
  );
  
  const advancedForm = (
    <div ref={advancedSearchPaneRef} role="dialog" aria-label="Advanced search form">
      <Panel variant='raised'>
        <PanelMain>
          <PanelMainBody>
            <Form>
              <FormGroup label='Has the words' fieldId='has-words' key='has-words'>
                <TextInput
                  type='text'
                  id='has-words'
                  value={hasWords}
                  onChange={value => {
                    setHasWords(value);
                    setValue(value);
                  }}
                  ref={firstAttrRef}
                />
              </FormGroup>
              <Grid hasGutter md={6}>
                <GridItem>
                  <FormGroup label='Date within' fieldId='date-within' key='date-within'>
                    <Popper trigger={dateWithinToggle} popper={dateWithinOptions} isVisible={isDateWithinOpen} />
                  </FormGroup>
                </GridItem>
                <GridItem>
                  <FormGroup label='Of date' fieldId='date' key='date'>
                    <DatePicker 
                      id="datePicker" 
                      style={{width: "100%"}} 
                      value={date} 
                      onChange={setDate} 
                      appendTo={() => document.querySelector("#datePicker")}
                    />
                  </FormGroup>
                </GridItem>
              </Grid>
              <ActionGroup>
                <Button variant="primary" type="submit" onClick={(e) => onSubmit(null, e)}>Submit</Button>
                {!!onClear && (
                  <Button variant="link" type="reset" onClick={onClear}>Reset</Button>
                )}
              </ActionGroup>
            </Form>
          </PanelMainBody>
        </PanelMain>
      </Panel>
    </div>
  );

  // Popper is just one way to build a relationship between a toggle and a menu.
  return (
    <Popper
      trigger={searchInput}
      popper={advancedForm}
      isVisible={isAdvancedSearchOpen}
      enableFlip={false}
      appendTo={() => document.querySelector("#custom-advanced-search")}
    />
  );
};
```
