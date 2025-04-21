import React from 'react';
import { render, screen } from '@testing-library/react';
import { MultipleFileUploadStatus } from '../MultipleFileUploadStatus';
import InProgressIcon from '@patternfly/react-icons/dist/esm/icons/in-progress-icon';

describe('MultipleFileUploadStatus', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadStatus>Foo</MultipleFileUploadStatus>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadStatus className="test">Foo</MultipleFileUploadStatus>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders status toggle text', () => {
    const { asFragment } = render(<MultipleFileUploadStatus statusToggleText="test">Foo</MultipleFileUploadStatus>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders status toggle icon', () => {
    const { asFragment } = render(
      <MultipleFileUploadStatus statusToggleIcon={<InProgressIcon />}>Foo</MultipleFileUploadStatus>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
