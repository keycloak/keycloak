import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';

import { TextInputGroupUtilities } from '../TextInputGroupUtilities';

describe('TextInputGroupUtilities', () => {
  it('renders without children', () => {
    render(<TextInputGroupUtilities data-testid="TextInputGroupUtilities" />);

    expect(screen.getByTestId('TextInputGroupUtilities')).toBeVisible();
  });

  it('renders the children', () => {
    render(<TextInputGroupUtilities>{<button>Test</button>}</TextInputGroupUtilities>);

    expect(screen.getByRole('button', { name: 'Test' })).toBeVisible();
  });

  it('renders with class pf-c-text-input-group__utilities', () => {
    render(<TextInputGroupUtilities>Test</TextInputGroupUtilities>);

    const utilities = screen.getByText('Test');

    expect(utilities).toHaveClass('pf-c-text-input-group__utilities');
  });

  it('matches the snapshot', () => {
    const { asFragment } = render(<TextInputGroupUtilities>{<button>Test</button>}</TextInputGroupUtilities>);

    expect(asFragment()).toMatchSnapshot();
  });
});
