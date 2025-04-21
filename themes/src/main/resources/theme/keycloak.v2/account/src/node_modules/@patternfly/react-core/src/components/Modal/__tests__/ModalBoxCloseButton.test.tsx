import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ModalBoxCloseButton } from '../ModalBoxCloseButton';

describe('ModalBoxCloseButton', () => {
  test('onClose called when clicked', () => {
    const onClose = jest.fn();

    render(<ModalBoxCloseButton className="test-box-close-button-class" onClose={onClose} />);

    userEvent.click(screen.getByRole('button'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
