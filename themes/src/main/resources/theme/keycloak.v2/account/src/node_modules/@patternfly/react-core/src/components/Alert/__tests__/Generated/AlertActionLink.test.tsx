/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AlertActionLink } from '../../AlertActionLink';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertActionLink should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<AlertActionLink children={'string'} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
