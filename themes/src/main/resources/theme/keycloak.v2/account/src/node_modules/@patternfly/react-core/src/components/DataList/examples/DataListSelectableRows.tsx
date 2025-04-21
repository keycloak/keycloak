import React from 'react';
import {
  Dropdown,
  DropdownItem,
  DropdownPosition,
  KebabToggle,
  DataList,
  DataListItem,
  DataListCell,
  DataListItemRow,
  DataListItemCells,
  DataListAction
} from '@patternfly/react-core';

export const DataListSelectableRows: React.FunctionComponent = () => {
  const [isOpen1, setIsOpen1] = React.useState(false);
  const [isOpen2, setIsOpen2] = React.useState(false);
  const [selectedDataListItemId, setSelectedDataListItemId] = React.useState('');

  const onToggle1 = isOpen1 => {
    setIsOpen1(isOpen1);
  };

  const onSelect1 = () => {
    setIsOpen1(!isOpen1);
  };

  const onToggle2 = isOpen2 => {
    setIsOpen2(isOpen2);
  };

  const onSelect2 = () => {
    setIsOpen2(!isOpen2);
  };

  const onSelectDataListItem = id => {
    setSelectedDataListItemId(id);
  };

  const handleInputChange = (id: string, _event: React.FormEvent<HTMLInputElement>) => {
    setSelectedDataListItemId(id);
  };

  return (
    <React.Fragment>
      <DataList
        aria-label="selectable data list example"
        selectedDataListItemId={selectedDataListItemId}
        onSelectDataListItem={onSelectDataListItem}
        selectableRow={{ type: 'single', onChange: handleInputChange }}
      >
        <DataListItem aria-labelledby="selectable-action-item1" id="item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="primary content">
                  <span id="selectable-action-item1">Single actionable Primary content</span>
                </DataListCell>,
                <DataListCell key="secondary content">Single actionable Secondary content</DataListCell>
              ]}
            />
            <DataListAction
              aria-labelledby="selectable-action-item1 selectable-action-action1"
              id="selectable-action-action1"
              aria-label="Actions"
              isPlainButtonAction
            >
              <Dropdown
                isPlain
                position={DropdownPosition.right}
                isOpen={isOpen1}
                onSelect={onSelect1}
                toggle={<KebabToggle onToggle={onToggle1} />}
                dropdownItems={[
                  <DropdownItem key="link">Link</DropdownItem>,
                  <DropdownItem key="action" component="button">
                    Action
                  </DropdownItem>,
                  <DropdownItem key="disabled link" isDisabled>
                    Disabled Link
                  </DropdownItem>
                ]}
              />
            </DataListAction>
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="selectable-actions-item2" id="item2">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="primary content">
                  <span id="selectable-actions-item2">Selectable actions Primary content</span>
                </DataListCell>,
                <DataListCell key="secondary content">Selectable actions Secondary content</DataListCell>
              ]}
            />
            <DataListAction
              aria-labelledby="selectable-actions-item2 selectable-actions-action2"
              id="selectable-actions-action2"
              aria-label="Actions"
              isPlainButtonAction
            >
              <Dropdown
                isPlain
                position={DropdownPosition.right}
                isOpen={isOpen2}
                onSelect={onSelect2}
                toggle={<KebabToggle onToggle={onToggle2} />}
                dropdownItems={[
                  <DropdownItem key="link">Link</DropdownItem>,
                  <DropdownItem key="action" component="button">
                    Action
                  </DropdownItem>,
                  <DropdownItem key="disabled link" isDisabled>
                    Disabled Link
                  </DropdownItem>
                ]}
              />
            </DataListAction>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    </React.Fragment>
  );
};
