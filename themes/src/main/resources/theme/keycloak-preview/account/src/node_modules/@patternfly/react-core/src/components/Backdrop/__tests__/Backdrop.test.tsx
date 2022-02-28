import React from 'react';
import { shallow } from 'enzyme';
import { Backdrop } from '../Backdrop';

test('Backdrop Test', () => {
  const view = shallow(<Backdrop>Backdrop</Backdrop>);
  expect(view).toMatchSnapshot();
});
