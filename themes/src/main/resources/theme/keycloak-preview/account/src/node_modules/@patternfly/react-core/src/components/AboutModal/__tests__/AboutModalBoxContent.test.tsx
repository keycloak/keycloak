import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxContent } from '../AboutModalBoxContent';

test('AboutModalBoxContent Test', () => {
  const view = shallow(
    <AboutModalBoxContent trademark="trademark" id="id">
      This is a AboutModalBoxContent
    </AboutModalBoxContent>
  );
  expect(view).toMatchSnapshot();
});
