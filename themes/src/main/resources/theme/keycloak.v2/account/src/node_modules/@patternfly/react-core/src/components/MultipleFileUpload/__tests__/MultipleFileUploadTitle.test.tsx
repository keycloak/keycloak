import React from 'react';
import { render, screen } from '@testing-library/react';
import { MultipleFileUploadTitle } from '../MultipleFileUploadTitle';

describe('MultipleFileUploadTitle', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadTitle />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadTitle className="test" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with title icon', () => {
    const { asFragment } = render(<MultipleFileUploadTitle icon="icon" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with title text', () => {
    const { asFragment } = render(<MultipleFileUploadTitle text="text" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with title text separator', () => {
    const { asFragment } = render(<MultipleFileUploadTitle text="text" textSeparator="text separator" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
