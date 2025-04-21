import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerGroup } from '../NotificationDrawerGroup';

describe('NotificationDrawerGroup', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerGroup count={2} isExpanded={false} title="Critical Alerts" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(
      <NotificationDrawerGroup
        count={2}
        isExpanded={false}
        title="Critical Alerts"
        className="extra-class"
        data-testid="test-id"
      />
    );
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  test('drawer group with isExpanded applied ', () => {
    const { asFragment } = render(<NotificationDrawerGroup count={2} isExpanded title="Critical Alerts" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('drawer group with isRead applied ', () => {
    const { asFragment } = render(
      <NotificationDrawerGroup count={2} isExpanded={false} isRead={true} title="Critical Alerts" />
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
