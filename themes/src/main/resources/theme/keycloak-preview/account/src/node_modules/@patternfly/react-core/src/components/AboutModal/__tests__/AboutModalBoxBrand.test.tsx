import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxBrand } from '../AboutModalBoxBrand';

test('test About Modal Brand', () => {
  const view = shallow(<AboutModalBoxBrand src="testimage.." alt="brand" />);
  expect(view).toMatchSnapshot();
});
