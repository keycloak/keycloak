/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Alert } from '../../Alert';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Alert should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Alert
      variant={'success'}
      isInline={false}
      title={<div>ReactNode</div>}
      action={null}
      children={''}
      className={"''"}
      aria-label={'string'}
      variantLabel={'string'}
      isLiveRegion={false}
    />
  );
  expect(view).toMatchSnapshot();
});
