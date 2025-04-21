import React from 'react';
import { Menu, MenuContent, MenuList, MenuItem } from '@patternfly/react-core';

export const MenuCheckboxList: React.FunctionComponent = () => {
  const [selectedItems, setSelectedItems] = React.useState<number[]>([]);

  /* eslint no-unused-vars: ["error", {"args": "after-used"}] */
  const onSelect = (event: React.MouseEvent<Element, MouseEvent>, itemId: number | string) => {
    const item = itemId as number;
    if (selectedItems.includes(item)) {
      setSelectedItems(selectedItems.filter(id => id !== item));
    } else {
      setSelectedItems([...selectedItems, item]);
    }
  };

  return (
    <Menu onSelect={onSelect} selected={selectedItems}>
      <MenuContent>
        <MenuList>
          <MenuItem hasCheck itemId={0} isSelected={selectedItems.includes(0)}>
            Checkbox 1
          </MenuItem>
          <MenuItem hasCheck itemId={1} isSelected={selectedItems.includes(1)}>
            Checkbox 2
          </MenuItem>
          <MenuItem hasCheck itemId={2} isDisabled>
            Checkbox 3
          </MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );
};
