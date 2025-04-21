import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Dropdown } from '../Dropdown';
import { DropdownPosition, DropdownDirection } from '../dropdownConstants';
import { InternalDropdownItem } from '../InternalDropdownItem';
import { DropdownSeparator } from '../DropdownSeparator';
import { DropdownToggle } from '../DropdownToggle';
import { KebabToggle } from '../KebabToggle';

const dropdownItems = [
  <InternalDropdownItem key="link">Link</InternalDropdownItem>,
  <InternalDropdownItem key="action" component="button">
    Action
  </InternalDropdownItem>,
  <InternalDropdownItem key="disabled link" isDisabled>
    Disabled Link
  </InternalDropdownItem>,
  <InternalDropdownItem key="disabled action" isDisabled component="button">
    Disabled Action
  </InternalDropdownItem>,
  <DropdownSeparator key="separator" />,
  <InternalDropdownItem key="separated link">Separated Link</InternalDropdownItem>,
  <InternalDropdownItem key="separated action" component="button">
    Separated Action
  </InternalDropdownItem>
];

describe('Dropdown', () => {
  test('regular', () => {
    const { asFragment } = render(
      <Dropdown dropdownItems={dropdownItems} toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('right aligned', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        position={DropdownPosition.right}
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('alignment breakpoints', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        alignments={{
          sm: 'left',
          md: 'right',
          lg: 'left',
          xl: 'right',
          '2xl': 'left'
        }}
        aria-label="Dropdown button"
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
        isOpen
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        direction={DropdownDirection.up}
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup + right aligned', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        direction={DropdownDirection.up}
        position={DropdownPosition.right}
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('expanded', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        isOpen
        aria-label="Dropdown button"
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('primary', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        toggle={
          <DropdownToggle id="Dropdown Toggle" isPrimary>
            Dropdown
          </DropdownToggle>
        }
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('basic', () => {
    const { asFragment } = render(
      <Dropdown isOpen toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}>
        <div>BASIC</div>
      </Dropdown>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('KebabToggle', () => {
  test('regular', () => {
    const { asFragment } = render(
      <Dropdown
        aria-label="Dropdown button"
        dropdownItems={dropdownItems}
        toggle={<KebabToggle id="Dropdown Toggle" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('right aligned', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        position={DropdownPosition.right}
        toggle={<KebabToggle id="Dropdown Toggle" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        direction={DropdownDirection.up}
        toggle={<KebabToggle id="Dropdown Toggle" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup + right aligned', () => {
    const { asFragment } = render(
      <Dropdown
        dropdownItems={dropdownItems}
        direction={DropdownDirection.up}
        position={DropdownPosition.right}
        toggle={<KebabToggle id="Dropdown Toggle" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('expanded', () => {
    const { asFragment } = render(
      <Dropdown
        aria-label="Dropdown button"
        dropdownItems={dropdownItems}
        isOpen
        toggle={<KebabToggle id="Dropdown Toggle" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('plain', () => {
    const { asFragment } = render(
      <Dropdown dropdownItems={dropdownItems} isPlain toggle={<KebabToggle id="Dropdown Toggle" />} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('basic', () => {
    const { asFragment } = render(
      <Dropdown isOpen toggle={<KebabToggle id="Dropdown Toggle" />}>
        <div>BASIC</div>
      </Dropdown>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('API', () => {
  test('click on item', () => {
    const mockToggle = jest.fn();
    const mockSelect = jest.fn();

    render(
      <Dropdown
        dropdownItems={dropdownItems}
        onSelect={mockSelect}
        isOpen
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );

    userEvent.click(screen.getByText('Link')); // "Link" is the text of the first item
    expect(mockToggle.mock.calls).toHaveLength(0);
    expect(mockSelect.mock.calls).toHaveLength(1);
  });

  test('dropdownItems and children console error ', () => {
    const myMock = jest.fn();
    global.console = { error: myMock } as any;
    render(
      <Dropdown
        dropdownItems={dropdownItems}
        isOpen
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      >
        <div>Children items</div>
      </Dropdown>
    );
    expect(myMock).toHaveBeenCalledWith(
      'Children and dropdownItems props have been provided. Only the dropdownItems prop items will be rendered'
    );
  });

  test('dropdownItems only, no console error ', () => {
    const myMock = jest.fn();
    global.console = { error: myMock } as any;
    render(
      <Dropdown
        dropdownItems={dropdownItems}
        isOpen
        toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}
      />
    );
    expect(myMock).not.toHaveBeenCalled();
  });

  test('children only, no console ', () => {
    const myMock = jest.fn();
    global.console = { error: myMock } as any;
    render(
      <Dropdown isOpen toggle={<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>}>
        <div>Children items</div>
      </Dropdown>
    );
    expect(myMock).not.toHaveBeenCalled();
  });
});
