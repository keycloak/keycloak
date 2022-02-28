import * as React from 'react';
import { shallow } from 'enzyme';
import { Avatar } from '../Avatar';

test('simple avatar', () => {
  const view: any = shallow(<Avatar alt="avatar" />);
  expect(view).toMatchSnapshot();
});
