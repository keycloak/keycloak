/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxHero } from '../../AboutModalBoxHero';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AboutModalBoxHero should match snapshot (auto-generated)', () => {
  const view = shallow(<AboutModalBoxHero className={'string'} backgroundImageSrc={'string'} />);
  expect(view).toMatchSnapshot();
});
