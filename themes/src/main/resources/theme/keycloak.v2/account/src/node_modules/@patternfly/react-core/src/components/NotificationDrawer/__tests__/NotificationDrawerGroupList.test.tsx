import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { NotificationDrawerGroupList } from '../NotificationDrawerGroupList';

describe('NotificationDrawerGroupList', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerGroupList />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<NotificationDrawerGroupList className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });
});
