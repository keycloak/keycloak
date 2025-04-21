import React from 'react';
import { render } from '@testing-library/react';
import { ClipboardCopyExpanded } from '../ClipboardCopyExpanded';

const props = {
  className: 'class-1',
  id: 'id-1'
};

test('expanded content render', () => {
  const { asFragment } = render(<ClipboardCopyExpanded {...props}>This is my text</ClipboardCopyExpanded>);
  expect(asFragment()).toMatchSnapshot();
});

test('expanded code content render', () => {
  const { asFragment } = render(
    <ClipboardCopyExpanded isCode {...props}>{`{
    "name": "@patternfly/react-core",
    "version": "1.33.2"
  }`}</ClipboardCopyExpanded>
  );
  expect(asFragment()).toMatchSnapshot();
});
