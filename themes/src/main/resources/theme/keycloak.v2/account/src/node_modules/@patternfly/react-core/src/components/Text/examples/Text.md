---
id: Text
section: components
cssPrefix: pf-c-content
propComponents: ['TextContent', 'Text', 'TextList', 'TextListItem']
---

## Examples

### Headings

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants,
  TextList,
  TextListVariants,
  TextListItem,
  TextListItemVariants
} from '@patternfly/react-core';

<TextContent>
  <Text component={TextVariants.h1}>Hello World</Text>
  <Text component={TextVariants.h2}>Second level</Text>
  <Text component={TextVariants.h3}>Third level</Text>
  <Text component={TextVariants.h4}>Fourth level</Text>
  <Text component={TextVariants.h5}>Fifth level</Text>
  <Text component={TextVariants.h6}>Sixth level</Text>
</TextContent>;
```

### Body

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants,
  TextList,
  TextListVariants,
  TextListItem,
  TextListItemVariants
} from '@patternfly/react-core';

<TextContent>
  <Text component={TextVariants.p}>
    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla accumsan, metus ultrices eleifend gravida, nulla nunc
    varius lectus, nec rutrum justo nibh eu lectus. Ut vulputate semper dui. Fusce erat odio, sollicitudin vel erat vel,
    interdum mattis neque. Sub works as well!
  </Text>
  <Text component={TextVariants.p}>
    Quisque ante lacus, malesuada ac auctor vitae, congue{' '}
    <Text component={TextVariants.a} href="#">
      non ante
    </Text>
    . Phasellus lacus ex, semper ac tortor nec, fringilla condimentum orci. Fusce eu rutrum tellus.
  </Text>
  <Text component={TextVariants.blockquote}>
    Ut venenatis, nisl scelerisque sollicitudin fermentum, quam libero hendrerit ipsum, ut blandit est tellus sit amet
    turpis.
  </Text>
  <Text component={TextVariants.small}>Sometimes you need small text to display things like date created</Text>
</TextContent>;
```

Text components such as Text, TextList, TextListItem need to be placed within a TextContent

### Unordered list

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants,
  TextList,
  TextListVariants,
  TextListItem,
  TextListItemVariants
} from '@patternfly/react-core';

<TextContent>
  <TextList>
    <TextListItem>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</TextListItem>
    <TextListItem>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</TextListItem>
    <TextListItem>
      Aliquam nec felis in sapien venenatis viverra fermentum nec lectus.
      <TextList>
        <TextListItem>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</TextListItem>
        <TextListItem>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</TextListItem>
      </TextList>
    </TextListItem>
    <TextListItem>Ut non enim metus.</TextListItem>
  </TextList>
</TextContent>;
```

### Ordered list

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants,
  TextList,
  TextListVariants,
  TextListItem,
  TextListItemVariants
} from '@patternfly/react-core';

<TextContent>
  <TextList component={TextListVariants.ol}>
    <TextListItem>Donec blandit a lorem id convallis.</TextListItem>
    <TextListItem>Cras gravida arcu at diam gravida gravida.</TextListItem>
    <TextListItem>Integer in volutpat libero.</TextListItem>
    <TextListItem>Donec a diam tellus.</TextListItem>
    <TextListItem>Aenean nec tortor orci.</TextListItem>
    <TextListItem>Quisque aliquam cursus urna, non bibendum massa viverra eget.</TextListItem>
    <TextListItem>Vivamus maximus ultricies pulvinar.</TextListItem>
  </TextList>
</TextContent>;
```

### Data list

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants,
  TextList,
  TextListVariants,
  TextListItem,
  TextListItemVariants
} from '@patternfly/react-core';

<TextContent>
  <TextList component={TextListVariants.dl}>
    <TextListItem component={TextListItemVariants.dt}>Web</TextListItem>
    <TextListItem component={TextListItemVariants.dd}>
      The part of the Internet that contains websites and web pages
    </TextListItem>
    <TextListItem component={TextListItemVariants.dt}>HTML</TextListItem>
    <TextListItem component={TextListItemVariants.dd}>A markup language for creating web pages</TextListItem>
    <TextListItem component={TextListItemVariants.dt}>CSS</TextListItem>
    <TextListItem component={TextListItemVariants.dd}>A technology to make HTML look better</TextListItem>
  </TextList>
</TextContent>;
```

### Visited

```js
import React from 'react';
import {
  TextContent,
  Text,
  TextVariants
} from '@patternfly/react-core';

TextVisited = () => {
  return (
    <>
      <TextContent>
        <Text component={TextVariants.h3}>Visited link example</Text>
        <Text component={TextVariants.p}>
          <Text 
            component={TextVariants.a} 
            isVisitedLink
            href="#">
            Visited link
          </Text>
        </Text>
      </TextContent>
      <br />
      <TextContent isVisited>
        <Text component={TextVariants.h3}>Visited content example</Text>
        <Text component={TextVariants.p}>
          <Text 
            component={TextVariants.a} 
            href="#">
            content link 1
          </Text>
        </Text>
        <Text component={TextVariants.p}>
          <Text 
            component={TextVariants.a} 
            href="#">
            content link 2
          </Text>
        </Text>
        <Text component={TextVariants.p}>
          <Text 
            component={TextVariants.a} 
            href="#">
            content link 3
          </Text>
        </Text>
      </TextContent>
    </>
  );

};
```
