import * as React from 'react';
import { render } from '@testing-library/react';
import { HelperText } from '../HelperText';
import { HelperTextItem } from '../HelperTextItem';
import { CheckIcon } from '@patternfly/react-icons';

describe('HelperText', () => {
  test('simple helper text renders successfully', () => {
    const { asFragment } = render(
      <HelperText>
        <HelperTextItem>help test text</HelperTextItem>
      </HelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  Object.values(['default', 'indeterminate', 'warning', 'success', 'invalid']).forEach(variant => {
    test(`${variant} helper text variant applies successfully`, () => {
      const { asFragment } = render(
        <HelperTextItem variant={variant as 'default' | 'indeterminate' | 'warning' | 'success' | 'error'}>
          {variant} help test text
        </HelperTextItem>
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });

  test('variant comonent helper text renders properly', () => {
    const { asFragment } = render(
      <HelperText component="ul">
        <HelperTextItem component="li">help test text 1</HelperTextItem>
        <HelperTextItem component="li">help test text 2</HelperTextItem>
      </HelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('icon helper text renders properly', () => {
    const { asFragment } = render(
      <HelperText>
        <HelperTextItem icon={<CheckIcon />}>help test text</HelperTextItem>
      </HelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('dynamic helper text renders successfully', () => {
    const { asFragment } = render(
      <HelperText>
        <HelperTextItem isDynamic>help test text</HelperTextItem>
      </HelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('helper text block renders successfully', () => {
    const { asFragment } = render(
      <HelperText>
        <HelperTextItem>help test text 1</HelperTextItem>
        <HelperTextItem>help test text 2</HelperTextItem>
        <HelperTextItem>help test text 3</HelperTextItem>
      </HelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
