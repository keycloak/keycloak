import { Badge } from '../Badge';
import React from 'react';
import { render } from '@testing-library/react';

Object.values([true, false]).forEach(isRead => {
  test(`${isRead} Badge`, () => {
    const { asFragment } = render(<Badge isRead={isRead}>{isRead ? 'read' : 'unread'} Badge</Badge>);
    expect(asFragment()).toMatchSnapshot();
  });
});
