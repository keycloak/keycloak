import { NotificationBadge } from '../NotificationBadge';
import React from 'react';
import { shallow } from 'enzyme';

Object.values([true, false]).forEach(isRead => {
  test(`${isRead} NotificationBadge`, () => {
    const view = shallow(<NotificationBadge isRead={isRead}>{isRead ? 'read' : 'unread'} Badge</NotificationBadge>);
    expect(view).toMatchSnapshot();
  });
});
