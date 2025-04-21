import * as React from 'react';
import { render } from '@testing-library/react';
import { SkipToContent } from '../SkipToContent';

const props = {};

test('Verify Skip To Content', () => {
  const { asFragment } = render(<SkipToContent href="#main-content" {...props} />);
  // Add a useful assertion here.
  expect(asFragment()).toMatchSnapshot();
});

test('Verify Skip To Content if forced to display', () => {
  const { asFragment } = render(<SkipToContent href="#main-content" {...props} show />);
  // Add a useful assertion here.
  expect(asFragment()).toMatchSnapshot();
});
