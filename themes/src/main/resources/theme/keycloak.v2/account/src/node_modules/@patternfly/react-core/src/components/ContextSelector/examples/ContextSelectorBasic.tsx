import React from 'react';
import { ContextSelector, ContextSelectorItem } from '@patternfly/react-core';

export const ContextSelectorBasic: React.FunctionComponent = () => {
  const items = [
    {
      text: 'Link',
      href: '#'
    },
    'Action',
    {
      text: 'Disabled link',
      href: '#',
      isDisabled: true
    },
    {
      text: 'Disabled action',
      isDisabled: true
    },
    'My project',
    'OpenShift cluster',
    'Production Ansible',
    'AWS',
    'Azure',
    'My project 2',
    'OpenShift cluster ',
    'Production Ansible 2 ',
    'AWS 2',
    'Azure 2'
  ];

  const firstItemText = typeof items[0] === 'string' ? items[0] : items[0].text;
  const [isOpen, setOpen] = React.useState(false);
  const [selected, setSelected] = React.useState(firstItemText);
  const [searchValue, setSearchValue] = React.useState('');
  const [filteredItems, setFilteredItems] = React.useState(items);

  const onToggle = (event: any, isOpen: boolean) => {
    setOpen(isOpen);
  };

  const onSelect = (event: any, value: React.ReactNode) => {
    setSelected(value as string);
    setOpen(!isOpen);
  };

  const onSearchInputChange = (value: string) => {
    setSearchValue(value);
  };

  const onSearchButtonClick = (_event: React.SyntheticEvent<HTMLButtonElement>) => {
    const filtered =
      searchValue === ''
        ? items
        : items.filter(item => {
            const str = typeof item === 'string' ? item : item.text;
            return str.toLowerCase().indexOf(searchValue.toLowerCase()) !== -1;
          });

    setFilteredItems(filtered || []);
  };
  return (
    <ContextSelector
      toggleText={selected}
      onSearchInputChange={onSearchInputChange}
      isOpen={isOpen}
      searchInputValue={searchValue}
      onToggle={onToggle}
      onSelect={onSelect}
      onSearchButtonClick={onSearchButtonClick}
      screenReaderLabel="Selected Project:"
    >
      {filteredItems.map((item, index) => {
        const [text = null, href = null, isDisabled] =
          typeof item === 'string' ? [item, null, false] : [item.text, item.href, item.isDisabled];
        return (
          <ContextSelectorItem key={index} href={href} isDisabled={isDisabled}>
            {text || item}
          </ContextSelectorItem>
        );
      })}
    </ContextSelector>
  );
};
