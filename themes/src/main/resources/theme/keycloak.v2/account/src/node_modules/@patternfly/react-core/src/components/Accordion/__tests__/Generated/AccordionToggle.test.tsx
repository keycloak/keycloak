/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AccordionToggle } from '../../AccordionToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AccordionToggle should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <AccordionToggle
      children={<>ReactNode</>}
      className={"''"}
      isExpanded={false}
      id={'string'}
      component={() => <div />}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
