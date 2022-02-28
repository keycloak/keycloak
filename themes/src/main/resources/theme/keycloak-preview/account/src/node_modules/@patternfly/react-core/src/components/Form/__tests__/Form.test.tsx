import React from 'react';
import { shallow } from 'enzyme';
import { Form } from '../Form';

describe('Form component', () => {
  test('should render default form variant', () => {
    const view = shallow(<Form />);
    expect(view).toMatchSnapshot();
  });

  test('should render horizontal form variant', () => {
    const view = shallow(<Form isHorizontal />);
    expect(view).toMatchSnapshot();
  });
});
