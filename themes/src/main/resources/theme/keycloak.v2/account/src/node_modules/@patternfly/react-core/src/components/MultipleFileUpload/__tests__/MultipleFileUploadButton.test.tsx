import React from 'react';
import { render } from '@testing-library/react';
import { MultipleFileUploadButton } from '../MultipleFileUploadButton';

describe('MultipleFileUploadButton', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadButton>Foo</MultipleFileUploadButton>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadButton className="test">Foo</MultipleFileUploadButton>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with aria-label applied to the button', () => {
    const { asFragment } = render(<MultipleFileUploadButton aria-label="test">Foo</MultipleFileUploadButton>);
    expect(asFragment()).toMatchSnapshot();
  });
});
