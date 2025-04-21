/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PageHeaderTools } from '../../PageHeaderTools';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PageHeaderTools should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PageHeaderTools children={<div>ReactNode</div>} className={'string'} />);
  expect(asFragment()).toMatchSnapshot();
});
