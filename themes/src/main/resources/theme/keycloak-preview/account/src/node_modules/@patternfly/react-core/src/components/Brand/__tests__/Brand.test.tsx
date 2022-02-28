import * as React from 'react';
import { shallow } from 'enzyme';
import { Brand } from '../Brand';

test('simple brand', () => {
  const view = shallow(<Brand alt="brand" />);
  expect(view).toMatchSnapshot();
});
