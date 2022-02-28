import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalContainer } from '../AboutModalContainer';

const props = {
  children: 'modal content',
  productName: 'Product Name',
  trademark: 'Trademark and copyright information here',
  brandImageSrc: 'brandImg...',
  brandImageAlt: 'Brand Image',
  backgroundImageSrc: 'backgroundImageSrc...',
  ariaLabelledbyId: 'ariaLablledbyId',
  ariaDescribedById: 'ariaDescribedById'
};
test('About Modal Container Test simple', () => {
  const view = shallow(<AboutModalContainer {...props}>This is ModalBox content</AboutModalContainer>);
  expect(view).toMatchSnapshot();
});

test('About Modal Container Test isOpen', () => {
  const view = shallow(
    <AboutModalContainer title="Test Modal Container title" {...props} isOpen>
      This is ModalBox content
    </AboutModalContainer>
  );
  expect(view).toMatchSnapshot();
});

test('About Modal Container Test with onlose', () => {
  const view = shallow(
    <AboutModalContainer onClose={() => undefined} {...props}>
      This is ModalBox content
    </AboutModalContainer>
  );
  expect(view).toMatchSnapshot();
});
