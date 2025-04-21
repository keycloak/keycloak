/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListContent } from '../../DataListContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListContent
      children={<>ReactNode</>}
      className={"''"}
      id={"''"}
      rowid={"''"}
      isHidden={false}
      hasNoPadding={false}
      aria-label={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
