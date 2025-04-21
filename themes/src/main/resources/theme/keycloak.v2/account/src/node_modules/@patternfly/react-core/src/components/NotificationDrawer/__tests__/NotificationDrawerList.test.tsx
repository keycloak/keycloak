import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerList } from '../NotificationDrawerList';

describe('NotificationDrawerList', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerList />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawerList className="extra-class" />);
    expect(screen.getByRole('list')).toHaveClass('extra-class');
  });

  test('drawer list with hidden applied ', () => {
    const { asFragment } = render(<NotificationDrawerList isHidden />);
    expect(asFragment()).toMatchSnapshot();
  });
});
