import React from 'react';
import { shallow } from 'enzyme';
import { SelectGroup } from '../SelectGroup';

describe('select group', () => {
  test('renders with children successfully', () => {
    const view = shallow(
      <SelectGroup label="test">
        <div>child</div>
      </SelectGroup>
    );
    expect(view).toMatchSnapshot();
  });
});
