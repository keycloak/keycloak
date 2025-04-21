import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerListItemBody } from '../NotificationDrawerListItemBody';

describe('NotificationDrawerListItemBody', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerListItemBody />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawerListItemBody className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  test('list item body with timestamp property applied', () => {
    const { asFragment } = render(<NotificationDrawerListItemBody timestamp="5 minutes ago" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
