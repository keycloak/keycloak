/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DropdownMenu } from '../../DropdownMenu';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DropdownMenu should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DropdownMenu
      children={<div>ReactNode</div>}
      className={"''"}
      isOpen={true}
      autoFocus={true}
      component={'ul'}
      position={'right'}
      isGrouped={false}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
