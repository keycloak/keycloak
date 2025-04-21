/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TextList } from '../../TextList';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TextList should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<TextList children={<>ReactNode</>} className={"''"} component={'ul'} />);
  expect(asFragment()).toMatchSnapshot();
});
