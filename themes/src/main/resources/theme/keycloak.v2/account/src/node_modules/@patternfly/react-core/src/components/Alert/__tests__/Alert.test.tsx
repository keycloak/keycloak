import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Alert, AlertVariant } from '../Alert';
import { AlertActionLink } from '../AlertActionLink';
import { AlertActionCloseButton } from '../AlertActionCloseButton';
import { UsersIcon } from '@patternfly/react-icons';

describe('Alert', () => {
  test('default Alert variant is default', () => {
    render(<Alert title="this is a test">Alert testing</Alert>);
    expect(screen.getByText('this is a test')).toHaveClass('pf-c-alert__title');
  });

  Object.values(AlertVariant).forEach(variant => {
    describe(`Alert - ${variant}`, () => {
      test('Description', () => {
        const { asFragment } = render(
          <Alert variant={variant} title="">
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Title', () => {
        const { asFragment } = render(
          <Alert variant={variant} title="Some title">
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Heading level', () => {
        const { asFragment } = render(
          <Alert variant={variant} title="Some title" titleHeadingLevel="h1">
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Action Link', () => {
        const { asFragment } = render(
          <Alert variant={variant} actionLinks={[<AlertActionLink key={'action-1'}>test</AlertActionLink>]} title="">
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Action Close Button', () => {
        const onClose = jest.fn();

        render(
          <Alert
            variant={variant}
            actionClose={<AlertActionCloseButton aria-label="Close" onClose={onClose} />}
            title={`Sample ${variant} alert`}
          >
            Some alert
          </Alert>
        );

        userEvent.click(screen.getByLabelText('Close'));
        expect(onClose).toHaveBeenCalled();
      });

      test('Action and Title', () => {
        const { asFragment } = render(
          <Alert
            variant={variant}
            actionLinks={[<AlertActionLink key={'action-1'}>test</AlertActionLink>]}
            title="Some title"
          >
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Custom aria label', () => {
        const { asFragment } = render(
          <Alert
            variant={variant}
            aria-label={`Custom aria label for ${variant}`}
            actionLinks={[<AlertActionLink key={'action-1'}>test</AlertActionLink>]}
            title="Some title"
          >
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('inline variation', () => {
        const { asFragment } = render(
          <Alert variant={variant} isInline title="Some title">
            Some alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('expandable variation', () => {
        const { asFragment } = render(
          <Alert variant={variant} title="Some title" isExpandable>
            <p>Success alert description. This should tell the user more information about the alert.</p>
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('expandable variation description hidden', () => {
        const description = 'Success alert description.';

        render(
          <Alert variant={variant} title="Some title" isExpandable>
            <p>{description}</p>
          </Alert>
        );

        expect(screen.queryByText(description)).toBeNull();
      });

      test('Toast alerts match snapsnot', () => {
        const { asFragment } = render(
          <Alert isLiveRegion={true} variant={variant} aria-label={`${variant} toast alert`} title="Some title">
            Some toast alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('Toast alerts contain default live region', () => {
        const ariaLabel = `${variant} toast alert`;

        render(
          <Alert isLiveRegion={true} variant={variant} aria-label={ariaLabel} title="Some title">
            Some toast alert
          </Alert>
        );

        expect(screen.getByLabelText(ariaLabel)).toHaveAttribute('aria-live', 'polite');
      });

      test('Toast alert live regions are not atomic', () => {
        const ariaLabel = `${variant} toast alert`;

        render(
          <Alert isLiveRegion={true} variant={variant} aria-label={ariaLabel} title="Some title">
            Some toast alert
          </Alert>
        );

        expect(screen.getByLabelText(ariaLabel)).toHaveAttribute('aria-atomic', 'false');
      });

      test('Non-toast alerts can have custom live region settings', () => {
        const ariaLabel = `${variant} toast alert`;

        render(
          <Alert
            aria-live="assertive"
            aria-relevant="all"
            aria-atomic="true"
            variant={variant}
            aria-label={ariaLabel}
            title="Some title"
          >
            Some noisy alert
          </Alert>
        );
        const alert = screen.getByLabelText(ariaLabel);

        expect(alert).toHaveAttribute('aria-live', 'assertive');
        expect(alert).toHaveAttribute('aria-relevant', 'all');
        expect(alert).toHaveAttribute('aria-atomic', 'true');
      });

      test('Custom icon', () => {
        const { asFragment } = render(
          <Alert
            customIcon={<UsersIcon />}
            variant={variant}
            aria-label={`${variant} custom icon alert`}
            title="custom icon alert title"
          >
            Some noisy alert
          </Alert>
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });
  });

  test('Alert truncate title', () => {
    render(
      <Alert truncateTitle={1} title="this is a test">
        Alert testing
      </Alert>
    );

    expect(screen.getByText('this is a test')).toHaveClass('pf-m-truncate');
  });
});
