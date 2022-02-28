/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SkipToContent } from '../../SkipToContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SkipToContent should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SkipToContent component={'a'} href={'string'} children={<div>ReactNode</div>} className={"''"} show={false} />
  );
  expect(view).toMatchSnapshot();
});
