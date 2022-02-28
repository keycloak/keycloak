import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBox } from '../AboutModalBox';

test('AboutModalBox Test', () => {
  const view = shallow(
    <AboutModalBox aria-labelledby="id" aria-describedby="id2">
      This is a AboutModalBox
    </AboutModalBox>
  );
  expect(view).toMatchSnapshot();
});
