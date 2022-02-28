import React from 'react';
import { shallow } from 'enzyme';
import { CheckboxSelectOption } from '../CheckboxSelectOption';

describe('checkbox select options', () => {
  test('renders with value parameter successfully', () => {
    const view = shallow(<CheckboxSelectOption value="test" sendRef={jest.fn()} />);
    expect(view).toMatchSnapshot();
  });

  test('renders with children successfully', () => {
    const view = shallow(
      <CheckboxSelectOption value="test" sendRef={jest.fn()}>
        <div>test</div>
      </CheckboxSelectOption>
    );
    expect(view).toMatchSnapshot();
  });

  describe('hover', () => {
    test('renders with checked successfully', () => {
      const view = shallow(<CheckboxSelectOption isChecked value="test" sendRef={jest.fn()} />);
      expect(view).toMatchSnapshot();
    });
  });

  describe('disabled', () => {
    test('renders disabled successfully', () => {
      const view = shallow(<CheckboxSelectOption isDisabled value="test" sendRef={jest.fn()} />);
      expect(view).toMatchSnapshot();
    });
  });
});
