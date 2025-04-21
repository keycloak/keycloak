import React from 'react';
import {
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
  Button,
  Menu,
  MenuContent,
  MenuList,
  MenuItem,
  Popper,
  Chip,
  ChipGroup,
  Divider
} from '@patternfly/react-core';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export const AutoCompleteSearch: React.FunctionComponent = () => {
  const [inputValue, setInputValue] = React.useState('');
  const [menuIsOpen, setMenuIsOpen] = React.useState(false);
  const [currentChips, setCurrentChips] = React.useState<string[]>([]);
  const [hint, setHint] = React.useState('');

  /** auto-completing suggestion text items to be shown in the menu */
  const suggestionItems = ['Cluster', 'Kind', 'Label', 'Name', 'Namespace', 'Status'];
  const [menuItems, setMenuItems] = React.useState<React.ReactElement[]>([]);

  /** refs used to detect when clicks occur inside vs outside of the textInputGroup and menu popper */
  const menuRef = React.useRef<HTMLDivElement>();
  const textInputGroupRef = React.useRef<HTMLDivElement>();

  /** callback for updating the inputValue state in this component so that the input can be controlled */
  const handleInputChange = (value: string, _event: React.FormEvent<HTMLInputElement>) => {
    setInputValue(value);
  };

  /** callback for removing a chip from the chip selections */
  const deleteChip = (chipToDelete: string) => {
    const newChips = currentChips.filter(chip => !Object.is(chip, chipToDelete));
    setCurrentChips(newChips);
  };

  /** callback for clearing all selected chips and the text input */
  const clearChipsAndInput = () => {
    setCurrentChips([]);
    setInputValue('');
  };

  React.useEffect(() => {
    /** in the menu only show items that include the text in the input */
    const filteredMenuItems = suggestionItems
      .filter(item => !inputValue || item.toLowerCase().includes(inputValue.toString().toLowerCase()))
      .map((currentValue, index) => (
        <MenuItem key={currentValue} itemId={index}>
          {currentValue}
        </MenuItem>
      ));

    /** in the menu show a disabled "no result" when all menu items are filtered out */
    if (filteredMenuItems.length === 0) {
      const noResultItem = (
        <MenuItem isDisabled key="no result">
          No results found
        </MenuItem>
      );
      setMenuItems([noResultItem]);
      setHint('');
      return;
    }

    /** The hint is set whenever there is only one autocomplete option left. */
    if (filteredMenuItems.length === 1) {
      const hint = filteredMenuItems[0].props.children;
      if (hint.toLowerCase().indexOf(inputValue.toLowerCase())) {
        // the match was found in a place other than the start, so typeahead wouldn't work right
        setHint('');
      } else {
        // use the input for the first part, otherwise case difference could make things look wrong
        setHint(inputValue + hint.substr(inputValue.length));
      }
    } else {
      setHint('');
    }

    /** add a heading to the menu */
    const headingItem = (
      <MenuItem isDisabled key="heading">
        Suggestions
      </MenuItem>
    );

    const divider = <Divider key="divider" />;

    setMenuItems([headingItem, divider, ...filteredMenuItems]);
  }, [inputValue]);

  /** add the given string as a chip in the chip group and clear the input */
  const addChip = (newChipText: string) => {
    setCurrentChips([...currentChips, `${newChipText}`]);
    setInputValue('');
  };

  /** add the current input value as a chip */
  const handleEnter = () => {
    if (inputValue.length) {
      addChip(inputValue);
    }
  };

  const handleTab = () => {
    if (menuItems.length === 3) {
      setInputValue(menuItems[2].props.children);
    }
    setMenuIsOpen(false);
  };

  /** close the menu when escape is hit */
  const handleEscape = () => {
    setMenuIsOpen(false);
  };

  /** allow the user to focus on the menu and navigate using the arrow keys */
  const handleArrowKey = () => {
    if (menuRef.current) {
      const firstElement = menuRef.current.querySelector<HTMLButtonElement>('li > button:not(:disabled)');
      firstElement && firstElement.focus();
    }
  };

  /** reopen the menu if it's closed and any un-designated keys are hit */
  const handleDefault = () => {
    if (!menuIsOpen) {
      setMenuIsOpen(true);
    }
  };

  /** enable keyboard only usage while focused on the text input */
  const handleTextInputKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'Enter':
        handleEnter();
        break;
      case 'Escape':
        handleEscape();
        break;
      case 'Tab':
        handleTab();
        break;
      case 'ArrowUp':
      case 'ArrowDown':
        handleArrowKey();
        break;
      default:
        handleDefault();
    }
  };

  /** apply focus to the text input */
  const focusTextInput = () => {
    textInputGroupRef.current.querySelector('input').focus();
  };

  /** add the text of the selected item as a new chip */
  const onSelect = (event: React.MouseEvent<Element, MouseEvent>, _itemId: string | number) => {
    const selectedText = (event.target as HTMLElement).innerText;
    addChip(selectedText);
    event.stopPropagation();
    focusTextInput();
  };

  /** close the menu when a click occurs outside of the menu or text input group */
  const handleClick = (event: MouseEvent) => {
    if (
      menuRef.current &&
      !menuRef.current.contains(event.target as HTMLElement) &&
      !textInputGroupRef.current.contains(event.target as HTMLElement)
    ) {
      setMenuIsOpen(false);
    }
  };

  /** enable keyboard only usage while focused on the menu */
  const handleMenuKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'Tab':
      case 'Escape':
        event.preventDefault();
        focusTextInput();
        setMenuIsOpen(false);
        break;
      case 'Enter':
      case ' ':
        setTimeout(() => setMenuIsOpen(false), 0);
        break;
    }
  };

  /** show the search icon only when there are no chips to prevent the chips from being displayed behind the icon */
  const showSearchIcon = !currentChips.length;

  /** only show the clear button when there is something that can be cleared */
  const showClearButton = !!inputValue || !!currentChips.length;

  /** render the utilities component only when a component it contains is being rendered */
  const showUtilities = showClearButton;

  const inputGroup = (
    <div ref={textInputGroupRef}>
      <TextInputGroup>
        <TextInputGroupMain
          icon={showSearchIcon && <SearchIcon />}
          value={inputValue}
          hint={hint}
          onChange={handleInputChange}
          onFocus={() => setMenuIsOpen(true)}
          onKeyDown={handleTextInputKeyDown}
          placeholder="search"
          aria-label="Search input"
        >
          <ChipGroup>
            {currentChips.map(currentChip => (
              <Chip key={currentChip} onClick={() => deleteChip(currentChip)}>
                {currentChip}
              </Chip>
            ))}
          </ChipGroup>
        </TextInputGroupMain>
        {showUtilities && (
          <TextInputGroupUtilities>
            {showClearButton && (
              <Button variant="plain" onClick={clearChipsAndInput} aria-label="Clear button for chips and input">
                <TimesIcon />
              </Button>
            )}
          </TextInputGroupUtilities>
        )}
      </TextInputGroup>
    </div>
  );

  const menu = (
    <div ref={menuRef}>
      <Menu onSelect={onSelect} onKeyDown={handleMenuKeyDown}>
        <MenuContent>
          <MenuList>{menuItems}</MenuList>
        </MenuContent>
      </Menu>
    </div>
  );

  return (
    <Popper
      trigger={inputGroup}
      popper={menu}
      appendTo={() => textInputGroupRef.current}
      isVisible={menuIsOpen}
      onDocumentClick={handleClick}
    />
  );
};
