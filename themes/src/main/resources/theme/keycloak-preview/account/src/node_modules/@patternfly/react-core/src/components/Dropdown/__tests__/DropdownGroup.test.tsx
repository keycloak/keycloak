import * as React from 'react';
import { shallow } from 'enzyme';
import { DropdownGroup } from '../DropdownGroup';

describe('dropdown groups', () => {
  test('basic render', () => {
    const view = shallow(<DropdownGroup label="Group 1">Something</DropdownGroup>);
    expect(view).toMatchSnapshot();
  });
});
