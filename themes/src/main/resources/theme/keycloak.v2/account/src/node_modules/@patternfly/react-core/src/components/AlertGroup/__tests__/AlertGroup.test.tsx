import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Alert } from '../../Alert';
import { AlertGroup } from '../../AlertGroup';
import { AlertActionCloseButton } from '../../../components/Alert/AlertActionCloseButton';

describe('AlertGroup', () => {
  test('Alert Group renders without children', () => {
    const { asFragment } = render(<AlertGroup />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Alert Group works with n children', () => {
    const { asFragment } = render(
      <AlertGroup>
        <Alert variant="success" title="alert title" />
        <Alert variant="warning" title="another alert title" />
      </AlertGroup>
    );
    expect(asFragment()).toBeTruthy();
  });

  test('Alert group overflow shows up', () => {
    const overflowMessage = 'View 2 more alerts';
    const onOverflowClick = jest.fn();

    render(
      <AlertGroup overflowMessage={overflowMessage} onOverflowClick={onOverflowClick}>
        <Alert variant="danger" title="alert title" />
      </AlertGroup>
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(2);

    const overflowButton = screen.getByRole('button', { name: 'View 2 more alerts' });
    expect(overflowButton).toBeInTheDocument();

    userEvent.click(overflowButton);
    expect(onOverflowClick).toHaveBeenCalled();
  });

  test('Standard Alert Group is not a toast alert group', () => {
    render(
      <AlertGroup>
        <Alert variant="danger" title="alert title" />
      </AlertGroup>
    );

    expect(screen.getByText('alert title').parentElement).not.toHaveClass('pf-m-toast');
  });

  test('Toast Alert Group contains expected modifier class', () => {
    render(
      <AlertGroup isToast aria-label="group label">
        <Alert variant="warning" title="alert title" />
      </AlertGroup>
    );

    expect(screen.getByLabelText('group label')).toHaveClass('pf-m-toast');
  });

  test('alertgroup closes when alerts are closed', () => {
    const onClose = jest.fn();

    render(
      <AlertGroup isToast appendTo={document.body}>
        <Alert
          isLiveRegion
          title={'Test Alert'}
          actionClose={<AlertActionCloseButton aria-label="Close" onClose={onClose} />}
        />
      </AlertGroup>
    );

    userEvent.click(screen.getByLabelText('Close'));
    expect(onClose).toHaveBeenCalled();
  });
});
