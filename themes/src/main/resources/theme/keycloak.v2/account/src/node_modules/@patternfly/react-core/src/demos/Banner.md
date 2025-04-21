---
id: Banner
section: components
---

import DashboardWrapper from './examples/DashboardWrapper';

## Demos

### Basic sticky banner

```js isFullscreen
import React from 'react';
import {
  Banner,
  Card,
  CardBody,
  Flex,
  Gallery,
  GalleryItem,
  PageSection,
  PageSectionVariants,
  TextContent,
  Text
} from '@patternfly/react-core';
import DashboardWrapper from './examples/DashboardWrapper';

class BannerDemo extends React.Component {
  render() {
    return (
      <React.Fragment>
        <DashboardWrapper breadcrumb={null}>
          <Banner isSticky>
            <Flex
              justifyContent={{ default: 'justifyContentCenter', lg: 'justifyContentSpaceBetween' }}
              flexWrap={{ default: 'nowrap' }}
            >
              <div className="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
              <div className="pf-u-display-none pf-u-display-block-on-lg">
                This message is sticky to the top of the page.
              </div>
              <div className="pf-u-display-none-on-lg">Drop some text on mobile, truncate if needed.</div>
              <div className="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
            </Flex>
          </Banner>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of it’s relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(30)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card key={i}>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </DashboardWrapper>
      </React.Fragment>
    );
  }
}
```

### Top and bottom banner

```js isFullscreen
import React from 'react';
import {
  Banner,
  Card,
  CardBody,
  Flex,
  FlexItem,
  Gallery,
  GalleryItem,
  PageSection,
  PageSectionVariants,
  TextContent,
  Text
} from '@patternfly/react-core';
import DashboardWrapper from '../examples/DashboardWrapper';

class BannerDemo extends React.Component {
  render() {
    return (
      <React.Fragment>
        <Flex
          direction={{ default: 'column' }}
          flexWrap={{ default: 'nowrap' }}
          spaceItems={{ default: 'spaceItemsNone' }}
          style={{ height: '100%' }}
        >
          <FlexItem>
            <Banner isSticky>
              <Flex
                justifyContent={{ default: 'justifyContentCenter', lg: 'justifyContentSpaceBetween' }}
                flexWrap={{ default: 'nowrap' }}
              >
                <div className="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
                <div className="pf-u-display-none pf-u-display-block-on-lg">
                  This message is sticky to the top of the page.
                </div>
                <div className="pf-u-display-none-on-lg">Drop some text on mobile, truncate if needed.</div>
                <div className="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
              </Flex>
            </Banner>
          </FlexItem>
          <FlexItem grow={{ default: 'grow' }} style={{ minHeight: 0 }}>
            <DashboardWrapper breadcrumb={null}>
              <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                  <Text component="h1">Main title</Text>
                  <Text component="p">
                    Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                    of it’s relative line height of 1.5.
                  </Text>
                </TextContent>
              </PageSection>
              <PageSection>
                <Gallery hasGutter>
                  {Array.apply(0, Array(30)).map((x, i) => (
                    <GalleryItem key={i}>
                      <Card key={i}>
                        <CardBody>This is a card</CardBody>
                      </Card>
                    </GalleryItem>
                  ))}
                </Gallery>
              </PageSection>
            </DashboardWrapper>
          </FlexItem>
          <FlexItem>
            <Banner isSticky>
              <Flex
                justifyContent={{ default: 'justifyContentCenter', lg: 'justifyContentSpaceBetween' }}
                flexWrap={{ default: 'nowrap' }}
              >
                <div className="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
                <div className="pf-u-display-none pf-u-display-block-on-lg">
                  This message is sticky to the bottom of the page.
                </div>
                <div className="pf-u-display-none-on-lg">Drop some text on mobile, truncate if needed.</div>
                <div className="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
              </Flex>
            </Banner>
          </FlexItem>
        </Flex>
      </React.Fragment>
    );
  }
}
```
