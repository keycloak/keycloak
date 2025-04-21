/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { KebabToggle } from '../../KebabToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('KebabToggle should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <KebabToggle
      id={"''"}
      children={<>ReactNode</>}
      className={"''"}
      isOpen={false}
      aria-label={"'Actions'"}
      onToggle={() => undefined as void}
      parentRef={null}
      isActive={false}
      isDisabled={false}
      isPlain={false}
      type={'button'}
      bubbleEvent={false}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
