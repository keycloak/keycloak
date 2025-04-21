import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ExpandableSection } from '../ExpandableSection';
import { ExpandableSectionToggle } from '../ExpandableSectionToggle';

const props = {};

test('ExpandableSection', () => {
  const { asFragment } = render(<ExpandableSection {...props}>test </ExpandableSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Renders ExpandableSection expanded', () => {
  const { asFragment } = render(<ExpandableSection isExpanded> test </ExpandableSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('ExpandableSection onToggle called', () => {
  const mockfn = jest.fn();

  render(<ExpandableSection onToggle={mockfn}> test </ExpandableSection>);

  userEvent.click(screen.getByRole('button'));
  expect(mockfn.mock.calls).toHaveLength(1);
});

test('Renders Uncontrolled ExpandableSection', () => {
  const { asFragment } = render(<ExpandableSection toggleText="Show More"> test </ExpandableSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Detached ExpandableSection renders successfully', () => {
  const { asFragment } = render(
    <React.Fragment>
      <ExpandableSection {...props} isExpanded isDetached contentId="test">
        test
      </ExpandableSection>
      <ExpandableSectionToggle isExpanded contentId="test" direction="up">
        Toggle text
      </ExpandableSectionToggle>
    </React.Fragment>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('Disclosure ExpandableSection', () => {
  const { asFragment } = render(
    <ExpandableSection {...props} displaySize="large" isWidthLimited>
      test{' '}
    </ExpandableSection>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('Renders ExpandableSection indented', () => {
  const { asFragment } = render(
    <ExpandableSection isExpanded isIndented>
      {' '}
      test{' '}
    </ExpandableSection>
  );
  expect(asFragment()).toMatchSnapshot();
});
