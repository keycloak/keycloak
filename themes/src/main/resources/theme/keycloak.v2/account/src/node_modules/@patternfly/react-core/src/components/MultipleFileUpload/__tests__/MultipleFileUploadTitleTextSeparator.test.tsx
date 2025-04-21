import React from 'react';
import { render, screen } from '@testing-library/react';
import { MultipleFileUploadTitleTextSeparator } from '../MultipleFileUploadTitleTextSeparator';

describe('MultipleFileUploadTitleTextSeparator', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadTitleTextSeparator>Foo</MultipleFileUploadTitleTextSeparator>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(
      <MultipleFileUploadTitleTextSeparator className="test">Foo</MultipleFileUploadTitleTextSeparator>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
