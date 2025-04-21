import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawer } from '../NotificationDrawer';

describe('NotificationDrawer', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawer />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawer className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });
});
