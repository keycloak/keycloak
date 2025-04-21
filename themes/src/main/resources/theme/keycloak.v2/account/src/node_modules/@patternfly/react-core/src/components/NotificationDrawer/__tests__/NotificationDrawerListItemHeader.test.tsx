import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import { NotificationDrawerListItemHeader } from '../NotificationDrawerListItemHeader';

describe('NotificationDrawerListItemHeader', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<NotificationDrawerListItemHeader title="Pod quit unexpectedly" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(
      <NotificationDrawerListItemHeader title="Pod quit unexpectedly" className="extra-class" data-testid="test-id" />
    );
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  test('list item header with custom icon applied ', () => {
    const { asFragment } = render(
      <NotificationDrawerListItemHeader title="Pod quit unexpectedly" icon={<BellIcon />} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('list item header with srTitle applied ', () => {
    const { asFragment } = render(
      <NotificationDrawerListItemHeader title="Pod quit unexpectedly" srTitle="screen reader title" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('list item header with variant applied ', () => {
    const { asFragment } = render(<NotificationDrawerListItemHeader title="Pod quit unexpectedly" variant="success" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('list item header with truncateTitle', () => {
    render(<NotificationDrawerListItemHeader truncateTitle={1} title="Pod quit unexpectedly" variant="success" />);

    expect(screen.getByText('Pod quit unexpectedly')).toHaveClass('pf-m-truncate');
  });
});
