import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerBody } from '../NotificationDrawerBody';

describe('NotificationDrawerBody', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerBody />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawerBody className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });
});
