import React from 'react';
import { render, screen } from '@testing-library/react';
import { MultipleFileUploadTitleText } from '../MultipleFileUploadTitleText';

describe('MultipleFileUploadTitleText', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadTitleText>Foo</MultipleFileUploadTitleText>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadTitleText className="test">Foo</MultipleFileUploadTitleText>);
    expect(asFragment()).toMatchSnapshot();
  });
});
