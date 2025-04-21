import React from 'react';

import { render } from '@testing-library/react';
import * as ReactCoreUtils from '@patternfly/react-core/src/helpers/util';

import { NavList } from '../../NavList';
import { NavContext } from '../../Nav';

describe('NavList', () => {
  beforeAll(() => {
    jest.spyOn(ReactCoreUtils, 'isElementInView').mockReturnValue(true);
  });

  it('should match snapshot', () => {
    const { asFragment } = render(
      <NavContext.Provider
        value={{
          onSelect: jest.fn(),
          onToggle: jest.fn(),
          updateIsScrollable: jest.fn(),
          isHorizontal: false,
          flyoutRef: undefined,
          setFlyoutRef: jest.fn()
        }}
      >
        <NavList children={<>ReactNode</>} className="" ariaLeftScroll="Scroll left" ariaRightScroll="Scroll right" />
      </NavContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
