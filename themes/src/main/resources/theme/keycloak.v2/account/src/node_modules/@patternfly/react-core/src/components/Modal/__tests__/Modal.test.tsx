import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { css } from '../../../../../react-styles/dist/js';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';

import { Modal } from '../Modal';

jest.spyOn(document, 'createElement');
jest.spyOn(document.body, 'addEventListener');

const props = {
  title: 'Modal',
  onClose: jest.fn(),
  isOpen: false,
  children: 'modal content'
};

describe('Modal', () => {
  test('Modal creates a container element once for div', () => {
    render(<Modal {...props} />);
    expect(document.createElement).toHaveBeenCalledWith('div');
  });

  test('modal closes with escape', () => {
    render(<Modal {...props} isOpen appendTo={document.body} />);

    userEvent.type(screen.getByText(props.title), '{esc}');
    expect(props.onClose).toHaveBeenCalled();
  });

  test('modal does not call onClose for esc key if it is not open', () => {
    render(<Modal {...props} />);
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(props.onClose).not.toHaveBeenCalled();
  });

  test('modal has body backdropOpen class when open', () => {
    render(<Modal {...props} isOpen />);
    expect(document.body).toHaveClass(css(styles.backdropOpen));
  });

  test('modal has no body backdropOpen class when not open', () => {
    render(<Modal {...props} />);
    expect(document.body).not.toHaveClass(css(styles.backdropOpen));
  });

  test('modal shows the close button when showClose is true (true by default)', () => {
    render(<Modal {...props} isOpen />);
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
  });

  test('modal does not show the close button when showClose is false', () => {
    render(<Modal {...props} isOpen showClose={false} tabIndex={0} />);
    expect(screen.queryByRole('button', { name: 'Close' })).toBeNull();
  });

  test('modal generates console error when no accessible name is provided', () => {
    const props = {
      onClose: jest.fn(),
      isOpen: true,
      children: 'modal content'
    };
    const consoleErrorMock = jest.fn();
    global.console = { error: consoleErrorMock } as any;

    render(<Modal {...props} />);

    expect(consoleErrorMock).toHaveBeenCalled();
  });

  test('modal generates console warning when conflicting accessible name strategies are provided', () => {
    const props = {
      hasNoBodyWrapper: true,
      onClose: jest.fn(),
      isOpen: true,
      children: 'modal content'
    };
    const consoleErrorMock = jest.fn();
    global.console = { error: consoleErrorMock } as any;

    render(<Modal {...props} />);

    expect(consoleErrorMock).toHaveBeenCalled();
  });

  test('The modalBoxBody has no aria-label when bodyAriaLabel is not passed', () => {
    const props = {
      isOpen: true
    };

    render(<Modal {...props}>This is a ModalBox</Modal>);

    const modalBoxBody = screen.getByText('This is a ModalBox');
    expect(modalBoxBody).not.toHaveAccessibleName('modal box body aria label');
  });

  test('The modalBoxBody has the expected aria-label when bodyAriaLabel is passed', () => {
    const props = {
      isOpen: true
    };

    render(
      <Modal bodyAriaLabel="modal box body aria label" {...props}>
        This is a ModalBox
      </Modal>
    );

    const modalBoxBody = screen.getByText('This is a ModalBox');
    expect(modalBoxBody).toHaveAccessibleName('modal box body aria label');
  });

  test('The modalBoxBody has the expected aria role when bodyAriaLabel is passed and bodyAriaRole is not', () => {
    const props = {
      isOpen: true
    };

    render(
      <Modal bodyAriaLabel="modal box body aria label" {...props}>
        This is a ModalBox
      </Modal>
    );

    const modalBoxBody = screen.getByRole('region', { name: 'modal box body aria label' });
    expect(modalBoxBody).toBeInTheDocument();
  });

  test('The modalBoxBody has the expected aria role when bodyAriaRole is passed', () => {
    const props = {
      isOpen: true
    };

    render(
      <Modal bodyAriaLabel="modal box body aria label" bodyAriaRole="article" {...props}>
        This is a ModalBox
      </Modal>
    );

    const modalBoxBody = screen.getByRole('article', { name: 'modal box body aria label' });
    expect(modalBoxBody).toBeInTheDocument();
  });
});
