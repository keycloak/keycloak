import React from 'react';
import { render } from '@testing-library/react';
import { MultipleFileUploadInfo } from '../MultipleFileUploadInfo';

describe('MultipleFileUploadInfo', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadInfo>Foo</MultipleFileUploadInfo>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadInfo className="test">Foo</MultipleFileUploadInfo>);
    expect(asFragment()).toMatchSnapshot();
  });
});
