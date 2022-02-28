import React from 'react';
import { mount } from 'enzyme';
import { HelpIcon } from '@patternfly/react-icons';
import { ApplicationLauncher } from '../ApplicationLauncher';
import { DropdownItem } from '../../Dropdown/DropdownItem';

import { DropdownPosition, DropdownDirection } from '../../Dropdown/dropdownConstants';
import { DropdownSeparator } from '../../Dropdown/DropdownSeparator';

const dropdownItems = [
  <DropdownItem key="link">Link</DropdownItem>,
  <DropdownItem key="action" component="button">
    Action
  </DropdownItem>,
  <DropdownItem key="disabled link" isDisabled>
    Disabled Link
  </DropdownItem>,
  <DropdownItem key="disabled action" isDisabled component="button">
    Disabled Action
  </DropdownItem>,
  <DropdownSeparator key="separator" />,
  <DropdownItem key="separated link">Separated Link</DropdownItem>,
  <DropdownItem key="separated action" component="button">
    Separated Action
  </DropdownItem>
];

describe('ApplicationLauncher', () => {
  test('regular', () => {
    const view = mount(<ApplicationLauncher dropdownItems={dropdownItems} />);
    expect(view).toMatchSnapshot();
  });

  test('right aligned', () => {
    const view = mount(<ApplicationLauncher dropdownItems={dropdownItems} position={DropdownPosition.right} />);
    expect(view).toMatchSnapshot();
  });

  test('dropup', () => {
    const view = mount(<ApplicationLauncher dropdownItems={dropdownItems} direction={DropdownDirection.up} />);
    expect(view).toMatchSnapshot();
  });

  test('dropup + right aligned', () => {
    const view = mount(
      <ApplicationLauncher
        dropdownItems={dropdownItems}
        direction={DropdownDirection.up}
        position={DropdownPosition.right}
      />
    );
    expect(view).toMatchSnapshot();
  });

  test('expanded', () => {
    const view = mount(<ApplicationLauncher dropdownItems={dropdownItems} isOpen />);
    expect(view).toMatchSnapshot();
  });

  test('custom icon', () => {
    const view = mount(
      <ApplicationLauncher dropdownItems={dropdownItems} isOpen toggleIcon={<HelpIcon id="test-icon" />} />
    );
    expect(view).toMatchSnapshot();
  });
});
