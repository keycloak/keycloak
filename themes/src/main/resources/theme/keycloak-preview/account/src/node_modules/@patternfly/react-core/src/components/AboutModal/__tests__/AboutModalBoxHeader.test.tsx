import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxHeader } from '../AboutModalBoxHeader';

test('AboutModalBoxHeader Test', () => {
  const view = shallow(
    <AboutModalBoxHeader productName="Product Name" id="id">
      This is a AboutModalBox header
    </AboutModalBoxHeader>
  );
  expect(view).toMatchSnapshot();
});
