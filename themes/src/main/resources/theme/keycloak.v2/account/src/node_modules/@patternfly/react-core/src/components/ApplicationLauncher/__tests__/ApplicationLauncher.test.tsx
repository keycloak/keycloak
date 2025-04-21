import React from 'react';
import { render } from '@testing-library/react';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import { ApplicationLauncher } from '../ApplicationLauncher';
import { ApplicationLauncherItem } from '../ApplicationLauncherItem';

import { DropdownPosition, DropdownDirection } from '../../Dropdown/dropdownConstants';
import { ApplicationLauncherSeparator } from '../ApplicationLauncherSeparator';

const dropdownItems = [
  <ApplicationLauncherItem key="link">Link</ApplicationLauncherItem>,
  <ApplicationLauncherItem key="action" component="button">
    Action
  </ApplicationLauncherItem>,
  <ApplicationLauncherItem key="disabled link" isDisabled>
    Disabled Link
  </ApplicationLauncherItem>,
  <ApplicationLauncherItem key="disabled action" isDisabled component="button">
    Disabled Action
  </ApplicationLauncherItem>,
  <ApplicationLauncherSeparator key="separator" />,
  <ApplicationLauncherItem key="separated link">Separated Link</ApplicationLauncherItem>,
  <ApplicationLauncherItem key="separated action" component="button">
    Separated Action
  </ApplicationLauncherItem>
];

describe('ApplicationLauncher', () => {
  test('regular', () => {
    const { asFragment } = render(<ApplicationLauncher items={dropdownItems} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('right aligned', () => {
    const { asFragment } = render(<ApplicationLauncher items={dropdownItems} position={DropdownPosition.right} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup', () => {
    const { asFragment } = render(<ApplicationLauncher items={dropdownItems} direction={DropdownDirection.up} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('dropup + right aligned', () => {
    const { asFragment } = render(
      <ApplicationLauncher items={dropdownItems} direction={DropdownDirection.up} position={DropdownPosition.right} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('expanded', () => {
    const { asFragment } = render(<ApplicationLauncher items={dropdownItems} isOpen />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('custom icon', () => {
    const { asFragment } = render(
      <ApplicationLauncher items={dropdownItems} isOpen toggleIcon={<HelpIcon id="test-icon" />} />
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
