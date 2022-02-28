import { Badge } from '../Badge';
import React from 'react';
import { shallow } from 'enzyme';

Object.values([true, false]).forEach(isRead => {
  test(`${isRead} Badge`, () => {
    const view = shallow(<Badge isRead={isRead}>{isRead ? 'read' : 'unread'} Badge</Badge>);
    expect(view).toMatchSnapshot();
  });
});
