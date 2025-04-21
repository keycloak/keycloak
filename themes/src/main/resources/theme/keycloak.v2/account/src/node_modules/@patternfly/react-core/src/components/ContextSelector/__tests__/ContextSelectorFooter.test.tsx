import React from 'react';
import { render } from '@testing-library/react';
import { ContextSelectorFooter } from '../ContextSelectorFooter';

test('Renders ContextSelectorFooter', () => {
  const { asFragment } = render(<ContextSelectorFooter>testing text</ContextSelectorFooter>);
  expect(asFragment()).toMatchSnapshot();
});
