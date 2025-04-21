/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { NavExpandable } from '../../NavExpandable';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('NavExpandable should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <NavExpandable
      title={'string'}
      srText={"''"}
      isExpanded={false}
      children={''}
      className={"''"}
      groupId={null}
      isActive={false}
      id={"''"}
      onExpand={() => undefined}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
