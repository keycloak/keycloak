/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListToggle } from '../../DataListToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListToggle should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListToggle
      className={"''"}
      isExpanded={false}
      id={'string'}
      rowid={"''"}
      aria-labelledby={"''"}
      aria-label={"'Details'"}
      aria-controls={"''"}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
