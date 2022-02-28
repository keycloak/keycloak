import * as React from 'react';
import { mount } from 'enzyme';
import { DropdownToggle } from '../DropdownToggle';
import { KebabToggle } from '../KebabToggle';

test('Dropdown toggle', () => {
  const view = mount(<DropdownToggle id="Dropdown Toggle">Dropdown</DropdownToggle>);
  expect(view).toMatchSnapshot();
});
test('Kebab toggle', () => {
  const view = mount(<KebabToggle id="Dropdown Toggle" />);
  expect(view).toMatchSnapshot();
});
