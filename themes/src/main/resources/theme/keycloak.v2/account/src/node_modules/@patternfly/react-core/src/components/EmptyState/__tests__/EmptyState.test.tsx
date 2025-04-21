import React from 'react';

import { render, screen } from '@testing-library/react';

import AddressBookIcon from '@patternfly/react-icons/dist/esm/icons/address-book-icon';
import { EmptyState, EmptyStateVariant } from '../EmptyState';
import { EmptyStateBody } from '../EmptyStateBody';
import { EmptyStateSecondaryActions } from '../EmptyStateSecondaryActions';
import { EmptyStateIcon } from '../EmptyStateIcon';
import { EmptyStatePrimary } from '../EmptyStatePrimary';
import { Button } from '../../Button';
import { Title, TitleSizes } from '../../Title';

describe('EmptyState', () => {
  test('Main', () => {
    const { asFragment } = render(
      <EmptyState>
        <Title headingLevel="h5" size="lg">
          HTTP Proxies
        </Title>
        <EmptyStateBody>
          Defining HTTP Proxies that exist on your network allows you to perform various actions through those proxies.
        </EmptyStateBody>
        <Button variant="primary">New HTTP Proxy</Button>
        <EmptyStateSecondaryActions>
          <Button variant="link" aria-label="learn more action">
            Learn more about this in the documentation.
          </Button>
        </EmptyStateSecondaryActions>
      </EmptyState>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Main variant large', () => {
    const { asFragment } = render(
      <EmptyState variant={EmptyStateVariant.large}>
        <Title headingLevel="h3" size={TitleSizes.md}>
          EmptyState large
        </Title>
      </EmptyState>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Main variant small', () => {
    const { asFragment } = render(
      <EmptyState variant={EmptyStateVariant.small}>
        <Title headingLevel="h3" size={TitleSizes.md}>
          EmptyState small
        </Title>
      </EmptyState>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Main variant xs', () => {
    const { asFragment } = render(
      <EmptyState variant={EmptyStateVariant.xs}>
        <Title headingLevel="h3" size={TitleSizes.md}>
          EmptyState small
        </Title>
      </EmptyState>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Body', () => {
    render(<EmptyStateBody className="custom-empty-state-body" data-testid="body-test-id" />);
    expect(screen.getByTestId('body-test-id')).toHaveClass('custom-empty-state-body pf-c-empty-state__body');
  });

  test('Secondary Action', () => {
    render(<EmptyStateSecondaryActions className="custom-empty-state-secondary" data-testid="actions-test-id" />);
    expect(screen.getByTestId('actions-test-id')).toHaveClass(
      'custom-empty-state-secondary pf-c-empty-state__secondary'
    );
  });

  test('Icon', () => {
    render(<EmptyStateIcon icon={AddressBookIcon} data-testid="icon-test-id" />);
    expect(screen.getByTestId('icon-test-id')).toHaveClass('pf-c-empty-state__icon');
  });

  test('Wrap icon in a div', () => {
    const { container } = render(
      <EmptyStateIcon
        variant="container"
        component={AddressBookIcon}
        className="custom-empty-state-icon"
        id="empty-state-icon-id"
      />
    );

    expect(container.querySelector('div')).toHaveClass('pf-c-empty-state__icon custom-empty-state-icon');
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  test('Primary div', () => {
    render(
      <EmptyStatePrimary data-testid="primary-test-id">
        <Button variant="link">Link</Button>
      </EmptyStatePrimary>
    );
    expect(screen.getByTestId('primary-test-id')).toHaveClass('pf-c-empty-state__primary');
  });

  test('Full height', () => {
    const { asFragment } = render(
      <EmptyState isFullHeight variant={EmptyStateVariant.large}>
        <Title headingLevel="h3" size={TitleSizes.md}>
          EmptyState large
        </Title>
      </EmptyState>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
