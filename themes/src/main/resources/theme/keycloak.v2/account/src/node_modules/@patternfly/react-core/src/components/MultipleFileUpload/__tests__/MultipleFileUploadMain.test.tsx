import React from 'react';
import { render } from '@testing-library/react';
import { MultipleFileUploadMain } from '../MultipleFileUploadMain';

describe('MultipleFileUploadMain', () => {
  test('renders with expected class names', () => {
    const { asFragment } = render(<MultipleFileUploadMain />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom class names', () => {
    const { asFragment } = render(<MultipleFileUploadMain className="test" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passes props to the title as expected', () => {
    const { asFragment } = render(
      <MultipleFileUploadMain titleIcon="icon" titleText="title text" titleTextSeparator="title test separator" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders without the button when expected', () => {
    const { asFragment } = render(<MultipleFileUploadMain isUploadButtonHidden />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passes props to the info component as expected', () => {
    const { asFragment } = render(<MultipleFileUploadMain infoText="info text" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
