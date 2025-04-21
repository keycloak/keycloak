import React from 'react';
import {
  DataList,
  DataListItem,
  DataListCell,
  DataListCheck,
  DataListAction,
  DataListToggle,
  DataListContent,
  DataListItemCells,
  DataListItemRow,
  Dropdown,
  DropdownItem,
  KebabToggle,
  DropdownPosition,
  Text,
  TextVariants,
  TextContent
} from '@patternfly/react-core';

export const DataListWidthModifiers: React.FunctionComponent = () => {
  const [show, setShow] = React.useState(true);
  const [isOpen1, setIsOpen1] = React.useState(false);
  const [isOpen2, setIsOpen2] = React.useState(false);

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

  const previewPlaceholder = {
    display: 'block',
    width: '100%',
    padding: '.25rem .5rem',
    color: '#004e8a',
    backgroundColor: '#def3ff',
    border: '1px solid rgba(0,0,0,.1)',
    borderRadius: '4px'
  };

  return (
    <>
      <div key="example-1">
        <TextContent>
          <Text component={TextVariants.h4}>Default fitting - example 1</Text>
        </TextContent>
        <DataList aria-label="Width modifier data list example 1">
          <DataListItem aria-labelledby="width-ex1-item1">
            <DataListItemRow>
              <DataListCheck aria-labelledby="width-ex1-item1" name="width-ex1-item1" />
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="default">
                    <div style={previewPlaceholder}>
                      <b id="width-ex1-item1">default</b>
                      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
                    </div>
                  </DataListCell>,
                  <DataListCell key="default2">
                    <div style={previewPlaceholder}>
                      <b>default</b>
                      <p>
                        Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut
                        labore et dolore magna aliqua.
                      </p>
                    </div>
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        </DataList>
      </div>
      <div key="example-2">
        <TextContent>
          <Text component={TextVariants.h4}>Flex modifiers - example 2</Text>
        </TextContent>
        <DataList aria-label="Width modifier data list example 2">
          <DataListItem aria-labelledby="width-ex2-item1">
            <DataListItemRow>
              <DataListCheck aria-labelledby="width-ex2-item1" name="width-ex2-item1" />
              <DataListItemCells
                dataListCells={[
                  <DataListCell width={2} key="width 2">
                    <div style={previewPlaceholder}>
                      <b id="width-ex2-item1">width 2</b>
                      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt.</p>
                    </div>
                  </DataListCell>,
                  <DataListCell width={4} key="width 4">
                    <div style={previewPlaceholder}>
                      <b>width 4</b>
                      <p>Lorem ipsum dolor sit amet.</p>
                    </div>
                  </DataListCell>
                ]}
              />
              <DataListAction
                aria-labelledby="width-ex2-item1 width-ex2-action1"
                id="width-ex2-action1"
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
        </DataList>
      </div>
      <div key="example-3">
        <TextContent>
          <Text component={TextVariants.h4}>Flex modifiers - example 3</Text>
        </TextContent>
        <DataList aria-label="Width modifier data list example 3">
          <DataListItem aria-labelledby="width-ex3-item1" isExpanded={show}>
            <DataListItemRow>
              <DataListToggle
                isExpanded={show}
                id="width-ex3-toggle1"
                aria-controls="width-ex3-expand1"
                onClick={() => setShow(!show)}
              />
              <DataListCheck aria-labelledby="width-ex3-item1" name="width-ex3-item1" />
              <DataListItemCells
                dataListCells={[
                  <DataListCell width={5} key="width 5">
                    <div style={previewPlaceholder}>
                      <b id="width-ex3-item1">width 5</b>
                      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
                    </div>
                  </DataListCell>,
                  <DataListCell width={2} key="width 2">
                    <div style={previewPlaceholder}>
                      <b>width 2</b>
                      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
                    </div>
                  </DataListCell>,
                  <DataListCell key="default">
                    <div style={previewPlaceholder}>default</div>
                  </DataListCell>
                ]}
              />
              <DataListAction
                aria-labelledby="width-ex3-item1 width-ex3-action1"
                id="width-ex3-action1"
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
            <DataListContent aria-label="Primary Content Details" id="width-ex3-expand1" isHidden={!show}>
              <p>
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua.
              </p>
            </DataListContent>
          </DataListItem>
        </DataList>
      </div>
    </>
  );
};
