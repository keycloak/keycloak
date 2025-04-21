import { NotificationBadge } from '../NotificationBadge';
import React from 'react';
import { render } from '@testing-library/react';

Object.values([true, false]).forEach(isRead => {
  test(`${isRead} NotificationBadge`, () => {
    const { asFragment } = render(<NotificationBadge isRead={isRead} />);
    expect(asFragment()).toMatchSnapshot();
  });
});

Object.values([true, false]).forEach(attentionVariant => {
  test(`${attentionVariant} NotificationBadge needs attention`, () => {
    const { asFragment } = render(
      <NotificationBadge variant="attention">
        {attentionVariant ? 'needs attention' : 'does not need attention'} Badge
      </NotificationBadge>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

test(`NotificationBadge count`, () => {
  const { asFragment } = render(<NotificationBadge variant="read" count={3} />);
  expect(asFragment()).toMatchSnapshot();
});
