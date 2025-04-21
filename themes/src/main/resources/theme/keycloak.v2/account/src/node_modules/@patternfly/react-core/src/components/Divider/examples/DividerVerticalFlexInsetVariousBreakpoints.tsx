import React from 'react';
import { Divider, Flex, FlexItem } from '@patternfly/react-core';

export const DividerVerticalFlexInsetVariousBreakpoints: React.FunctionComponent = () => (
  <Flex>
    <FlexItem>first item</FlexItem>
    <Divider
      orientation={{
        default: 'vertical'
      }}
      inset={{
        default: 'insetMd',
        md: 'insetNone',
        lg: 'insetSm',
        xl: 'insetXs'
      }}
    />
    <FlexItem>second item</FlexItem>
  </Flex>
);
