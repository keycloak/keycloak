/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { GalleryItem } from '../../GalleryItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('GalleryItem should match snapshot (auto-generated)', () => {
  const view = shallow(<GalleryItem children={<>ReactNode</>} />);
  expect(view).toMatchSnapshot();
});
