/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AccordionContent } from '../../AccordionContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AccordionContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <AccordionContent
      children={<>ReactNode</>}
      className={"''"}
      id={"''"}
      isHidden={false}
      isFixed={false}
      aria-label={"''"}
      component={() => <div />}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
