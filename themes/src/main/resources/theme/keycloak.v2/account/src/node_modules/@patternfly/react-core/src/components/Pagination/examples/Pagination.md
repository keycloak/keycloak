---
id: Pagination
section: components
cssPrefix: null
propComponents: ['Pagination', PaginationTitles, PerPageOptions, ToggleTemplateProps]
ouia: true
---

## Examples

### Top

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationTop extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={523}
        perPage={this.state.perPage}
        page={this.state.page}
        onSetPage={this.onSetPage}
        widgetId="pagination-options-menu-top"
        onPerPageSelect={this.onPerPageSelect}
      />
    );
  }
}
```

### Bottom

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationBottom extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1
    };
    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };
    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={333}
        widgetId="pagination-options-menu-bottom"
        perPage={this.state.perPage}
        page={this.state.page}
        variant={PaginationVariant.bottom}
        onSetPage={this.onSetPage}
        onPerPageSelect={this.onPerPageSelect}
      />
    );
  }
}
```

### Indeterminate

By not passing `itemCount` and passing `toggleTemplate` you can customize the toggle with text.

```js
import React from 'react';
import { Pagination } from '@patternfly/react-core';

PaginationIndeterminate = () => {
  const [page, setPage] = React.useState(1);
  const [perPage, setPerPage] = React.useState(20);

  const onSetPage = (_event, pageNumber) => setPage(pageNumber);
  const onPerPageSelect = (_event, perPage) => setPerPage(perPage);

  return (
    <Pagination
      perPageComponent="button"
      toggleTemplate={({ firstIndex, lastIndex }) => (
        <React.Fragment>
          <b>
            {firstIndex} - {lastIndex}
          </b>
          of
          <b>many</b>
        </React.Fragment>
      )}
      widgetId="pagination-indeterminate"
      perPage={perPage}
      page={page}
      onSetPage={onSetPage}
      onPerPageSelect={onPerPageSelect}
    />
  );
};
```

### Disabled

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationDisabled extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={523}
        perPage={this.state.perPage}
        page={this.state.page}
        onSetPage={this.onSetPage}
        widgetId="pagination-options-menu-top"
        onPerPageSelect={this.onPerPageSelect}
        isDisabled
      />
    );
  }
}
```

### No items

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationTop extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={0}
        perPage={this.state.perPage}
        page={this.state.page}
        onSetPage={this.onSetPage}
        widgetId="pagination-options-menu-top"
        onPerPageSelect={this.onPerPageSelect}
      />
    );
  }
}
```

### One page

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationTop extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={15}
        perPage={this.state.perPage}
        page={this.state.page}
        onSetPage={this.onSetPage}
        widgetId="pagination-options-menu-top"
        onPerPageSelect={this.onPerPageSelect}
      />
    );
  }
}
```

### Compact

```js
import React from 'react';
import { Pagination, PaginationVariant } from '@patternfly/react-core';

class PaginationTop extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <Pagination
        perPageComponent="button"
        itemCount={523}
        perPage={this.state.perPage}
        page={this.state.page}
        onSetPage={this.onSetPage}
        widgetId="pagination-options-menu-top"
        onPerPageSelect={this.onPerPageSelect}
        isCompact
      />
    );
  }
}
```

### Sticky

```js isFullscreen
import React from 'react';
import { Pagination, PaginationVariant, Gallery, GalleryItem, Card, CardBody } from '@patternfly/react-core';

class PaginationSticky extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 1,
      perPage: 20,
      isTopSticky: true
    };

    this.onToggleSticky = () => {
      this.setState({
        isTopSticky: !this.state.isTopSticky
      });
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };
  }

  render() {
    return (
      <div>
        {this.state.isTopSticky && (
          <React.Fragment>
            <Pagination
              perPageComponent="button"
              itemCount={523}
              perPage={this.state.perPage}
              page={this.state.page}
              onSetPage={this.onSetPage}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={this.onPerPageSelect}
              isSticky
            >
              <button onClick={this.onToggleSticky}>Toggle to bottom position</button>
            </Pagination>
            <Gallery hasGutter>
              {Array.apply(0, Array(40)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </React.Fragment>
        )}
        {!this.state.isTopSticky && (
          <React.Fragment>
            <Gallery hasGutter>
              {Array.apply(0, Array(40)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
            <Pagination
              perPageComponent="button"
              itemCount={523}
              perPage={this.state.perPage}
              page={this.state.page}
              onSetPage={this.onSetPage}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={this.onPerPageSelect}
              isSticky
              variant={PaginationVariant.bottom}
            >
              <button onClick={this.onToggleSticky}>Toggle to top position</button>
            </Pagination>
          </React.Fragment>
        )}
      </div>
    );
  }
}
```
