import React from 'react';
import { shallow } from 'enzyme';
import { CheckboxSelectGroup } from '../CheckboxSelectGroup';

describe('checkbox select options', () => {
  test('renders with children successfully', () => {
    const view = shallow(
      <CheckboxSelectGroup label="test">
        <div>child</div>
      </CheckboxSelectGroup>
    );
    expect(view).toMatchSnapshot();
  });
});
