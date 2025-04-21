import * as React from 'react';
import { Gallery } from '../Gallery';
import { GalleryItem } from '../GalleryItem';
import { render } from '@testing-library/react';

test('gutter', () => {
  const { asFragment } = render(<Gallery hasGutter />);
  expect(asFragment()).toMatchSnapshot();
});

test('gutter breakpoints', () => {
  const { asFragment } = render(
    <Gallery
      hasGutter
      minWidths={{
        default: '100%',
        md: '100px',
        xl: '300px'
      }}
      maxWidths={{
        md: '200px',
        xl: '1fr'
      }}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});

test('alternative component', () => {
  const { asFragment } = render(
    <Gallery component="ul">
      <GalleryItem component="li">Test</GalleryItem>
    </Gallery>
  );
  expect(asFragment()).toMatchSnapshot();
});
