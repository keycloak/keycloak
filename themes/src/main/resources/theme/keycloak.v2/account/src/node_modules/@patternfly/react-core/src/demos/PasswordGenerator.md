---
id: Password generator
section: demos
---

import RedoIcon from '@patternfly/react-icons/dist/esm/icons/redo-icon';
import EyeIcon from '@patternfly/react-icons/dist/esm/icons/eye-icon';
import EyeSlashIcon from '@patternfly/react-icons/dist/esm/icons/eye-slash-icon';

## Demos

### Provide a generated password

```ts
import React from 'react';
import {
  InputGroup,
  TextInput,
  Button,
  Popper,
  Menu,
  MenuContent,
  MenuList,
  MenuItem,
  MenuItemAction
} from '@patternfly/react-core';
import RedoIcon from '@patternfly/react-icons/dist/esm/icons/redo-icon';
import EyeIcon from '@patternfly/react-icons/dist/esm/icons/eye-icon';
import EyeSlashIcon from '@patternfly/react-icons/dist/esm/icons/eye-slash-icon';

const PasswordGenerator: React.FunctionComponent = () => {
  const generatePassword = () => {
    const length = 12;
    const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@%()_-=+';
    let retVal = '';
    for (var i = 0, n = charset.length; i < length; ++i) {
      retVal += charset.charAt(Math.floor(Math.random() * n));
    }
    return retVal;
  };
  const [password, setPassword] = React.useState<string>('');
  const [generatedPassword, setGeneratedPassword] = React.useState<string>(generatePassword());
  const [isAutocompleteOpen, setIsAutocompleteOpen] = React.useState<boolean>(false);
  const [passwordHidden, setPasswordHidden] = React.useState<boolean>(true);
  const searchInputRef = React.useRef(null);
  const autocompleteRef = React.useRef(null);

  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isAutocompleteOpen, searchInputRef.current]);

  const onChange = (newValue: string) => {
    if (searchInputRef && searchInputRef.current && searchInputRef.current.contains(document.activeElement)) {
      setIsAutocompleteOpen(true);
    } else {
      setIsAutocompleteOpen(false);
    }
    setPassword(newValue);
  };

  // Whenever an autocomplete option is selected, set the search input value, close the menu, and put the browser
  // focus back on the search input
  const onSelect = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    setPassword(generatedPassword);
    setIsAutocompleteOpen(false);
    searchInputRef.current.focus();
  };

  const handleMenuKeys = (event: KeyboardEvent | React.KeyboardEvent<any>) => {
    if (!(isAutocompleteOpen && searchInputRef.current && searchInputRef.current.contains(event.target))) {
      return;
    }
    // the escape key closes the autocomplete menu and keeps the focus on the search input.
    if (event.key === 'Escape') {
      setIsAutocompleteOpen(false);
      searchInputRef.current.focus();
      // the up and down arrow keys move browser focus into the autocomplete menu
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      const firstElement = autocompleteRef.current.querySelector('li > button:not(:disabled)');
      firstElement && firstElement.focus();
      event.preventDefault(); // by default, the up and down arrow keys scroll the window
    }
    // If the autocomplete is open and the browser focus is in the autocomplete menu
    // hitting tab will close the autocomplete and put browser focus back on the search input.
    else if (autocompleteRef.current.contains(event.target) && event.key === 'Tab') {
      event.preventDefault();
      setIsAutocompleteOpen(false);
      searchInputRef.current.focus();
    }
  };

  // The autocomplete menu should close if the user clicks outside the menu.
  const handleClickOutside = (event: MouseEvent | TouchEvent | KeyboardEvent | React.KeyboardEvent<any> | React.MouseEvent<HTMLButtonElement>) => {
    if (
      isAutocompleteOpen &&
      autocompleteRef &&
      autocompleteRef.current &&
      !searchInputRef.current.contains(event.target)
    ) {
      setIsAutocompleteOpen(false);
    }
    if (
      !isAutocompleteOpen &&
      searchInputRef &&
      searchInputRef.current &&
      searchInputRef.current.contains(event.target)
    ) {
      setIsAutocompleteOpen(true);
    }
  };
  const textInput = (
    <div ref={searchInputRef} id="password-input">
      <InputGroup>
        <TextInput
          onFocus={() => {
            setIsAutocompleteOpen(true);
          }}
          isRequired
          type={passwordHidden ? 'password' : 'text'}
          aria-label="Password input"
          value={password}
          onChange={onChange}
        />
        <Button
          variant="control"
          onClick={() => setPasswordHidden(!passwordHidden)}
          aria-label={passwordHidden ? 'Show password' : 'Hide password'}
        >
          {passwordHidden ? <EyeIcon /> : <EyeSlashIcon />}
        </Button>
      </InputGroup>
    </div>
  );
  const autocomplete = (
    <Menu ref={autocompleteRef} onSelect={onSelect}>
      <MenuContent>
        <MenuList>
          <MenuItem
            itemId={0}
            actions={
              <MenuItemAction
                icon={<RedoIcon aria-hidden />}
                onClick={e => {
                  setGeneratedPassword(generatePassword());
                }}
                actionId="redo"
                aria-label="Generate a new suggested password"
              />
            }
          >
            Use suggested password: <b>{`${generatedPassword}`}</b>
          </MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );

  return (
    <Popper
      trigger={textInput}
      popper={autocomplete}
      isVisible={isAutocompleteOpen}
      enableFlip={false}
      // append the autocomplete menu to the search input in the DOM for the sake of the keyboard navigation experience
      appendTo={() => document.querySelector('#password-input')}
    />
  );
};
```
