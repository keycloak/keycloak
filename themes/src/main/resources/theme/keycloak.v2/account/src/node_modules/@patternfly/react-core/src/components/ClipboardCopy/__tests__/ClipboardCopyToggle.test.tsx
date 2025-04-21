import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ClipboardCopyToggle, ClipboardCopyToggleProps } from '../ClipboardCopyToggle';

const props: ClipboardCopyToggleProps = {
  id: 'my-id',
  textId: 'my-text-id',
  contentId: 'my-content-id',
  isExpanded: false,
  className: 'myclassName',
  onClick: jest.fn()
};

describe('ClipboardCopyToggle', () => {
  test('toggle button render', () => {
    const desc = 'toggle content';
    const { asFragment } = render(<ClipboardCopyToggle {...props} aria-label={desc} />);

    expect(asFragment()).toMatchSnapshot();
  });

  test('toggle button onClick', () => {
    const onclick = jest.fn();
    render(<ClipboardCopyToggle {...props} onClick={onclick} />);

    userEvent.click(screen.getByRole('button'));
    expect(onclick).toHaveBeenCalled();
  });

  test('has aria-expanded set to true when isExpanded is true', () => {
    render(<ClipboardCopyToggle {...props} isExpanded />);

    const toggleButton = screen.getByRole('button');
    expect(toggleButton).toHaveAttribute('aria-expanded', 'true');
  });

  test('has aria-expanded set to false when isExpanded is false', () => {
    render(<ClipboardCopyToggle {...props} />);

    const toggleButton = screen.getByRole('button');
    expect(toggleButton).toHaveAttribute('aria-expanded', 'false');
  });
});
