/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { CardActions } from '../../CardActions';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CardActions should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<CardActions children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
