import * as React from 'react';
import { render, screen } from '@testing-library/react';
import { Brand } from '../Brand';
import '@testing-library/jest-dom';

describe('Brand', () => {
  test('simple brand', () => {
    const { asFragment } = render(<Brand alt="brand" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing children creates picture brand', () => {
    render(
      <Brand alt="brand">
        <div>test</div>
      </Brand>
    );
    expect(screen.getByText('test')).toBeInTheDocument();
  });
});
