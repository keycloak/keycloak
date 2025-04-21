import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalContainer } from '../AboutModalContainer';

const props = {
  children: 'modal content',
  productName: 'Product Name',
  trademark: 'Trademark and copyright information here',
  brandImageSrc: 'brandImg...',
  brandImageAlt: 'Brand Image',
  backgroundImageSrc: 'backgroundImageSrc...',
  'aria-labelledby': 'ariaLablledbyId',
  'aria-describedby': 'ariaDescribedById',
  aboutModalBoxHeaderId: 'header-id',
  aboutModalBoxContentId: 'content-id'
};
test('About Modal Container Test simple', () => {
  const { asFragment } = render(<AboutModalContainer {...props}>This is ModalBox content</AboutModalContainer>);
  expect(asFragment()).toMatchSnapshot();
});

test('About Modal Container Test isOpen', () => {
  const { asFragment } = render(
    <AboutModalContainer title="Test Modal Container title" {...props} isOpen>
      This is ModalBox content
    </AboutModalContainer>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('About Modal Container Test with onlose', () => {
  const { asFragment } = render(
    <AboutModalContainer onClose={() => undefined} {...props}>
      This is ModalBox content
    </AboutModalContainer>
  );
  expect(asFragment()).toMatchSnapshot();
});
