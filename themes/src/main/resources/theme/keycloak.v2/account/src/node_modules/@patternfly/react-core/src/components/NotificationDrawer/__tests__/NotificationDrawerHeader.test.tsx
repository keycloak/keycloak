import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerHeader } from '../NotificationDrawerHeader';

describe('NotificationDrawerHeader', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerHeader />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawerHeader className="extra-class" />);
    expect(screen.getByText('Notifications').parentElement).toHaveClass('extra-class');
  });

  test('drawer header with count applied', () => {
    const { asFragment } = render(<NotificationDrawerHeader count={2} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('drawer header with title applied', () => {
    const { asFragment } = render(<NotificationDrawerHeader title="Some Title" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('drawer header with custom unread text applied', () => {
    const { asFragment } = render(<NotificationDrawerHeader customText="2 unread alerts" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
