import React from 'react';
import { List, ListItem } from '@patternfly/react-core';

export const ListHorizontalRules: React.FunctionComponent = () => (
  <List isPlain isBordered>
    <ListItem>First</ListItem>
    <ListItem>Second</ListItem>
    <ListItem>Third</ListItem>
  </List>
);
