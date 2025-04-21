/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { GalleryItem } from '../../GalleryItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('GalleryItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<GalleryItem children={<>ReactNode</>} />);
  expect(asFragment()).toMatchSnapshot();
});
