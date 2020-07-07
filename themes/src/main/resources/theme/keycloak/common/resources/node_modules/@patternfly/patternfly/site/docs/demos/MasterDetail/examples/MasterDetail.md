---
title: Master Detail
section: demos
wrapperTag: div
---

## Demos

```hbs title=Master-detail-expanded isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-expanded-example"
  master-detail-template--title="Master detail expanded"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  master-detail-template--modifier="pf-m-light"
  }}

  {{#> divider divider--type="div"}}{{/divider}}
  {{#> page-main-section page-main-section--modifier="pf-m-no-padding"}}

    <!-- Drawer -->
    {{#> drawer
      drawer--id=(concat master-detail-template--id '-drawer')
      drawer-panel--IsOpen="true"
      drawer--modifier="pf-m-inline-on-2xl"
      }}

      {{#> drawer-main}}

        <!-- Content -->
        {{#> drawer-content}}
          {{> master-detail-toolbar}}
          {{> master-detail-data-list}}
        {{/drawer-content}}

        <!-- Panel -->
        {{#> drawer-panel
          drawer-panel--NoBody="true"
          progress--modifier="pf-m-sm"
          }}

          <!-- Panel header -->
          {{> master-detail-panel-header
            master-detail-panel-header--title="Node 2"
            master-detail-panel-header--sub-title='<a href="#">siemur/test-space</a>'
            }}

          <!-- Tabs -->
          {{#> drawer-body drawer-body--modifier="pf-m-no-padding"}}
            {{> master-detail-panel-tabs}}
          {{/drawer-body}}

          <!-- Tab content -->
          {{#> drawer-body}}
            {{> master-detail-panel-tab-content}}
          {{/drawer-body}}
        {{/drawer-panel}}
      {{/drawer-main}}
    {{/drawer}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Master-detail-collapsed isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-collapsed-example"
  master-detail-template--title="Master detail collapsed"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  master-detail-template--modifier="pf-m-light"
  }}

  {{#> divider divider--type="div"}}{{/divider}}
  {{#> page-main-section page-main-section--modifier="pf-m-no-padding"}}

    <!-- Drawer -->
    {{#> drawer
      drawer--id=(concat master-detail-template--id '-drawer')
      drawer--modifier="pf-m-inline-on-2xl"
      }}

      {{#> drawer-main}}

        <!-- Content -->
        {{#> drawer-content}}
          {{> master-detail-toolbar}}
          {{> master-detail-data-list}}
        {{/drawer-content}}


        <!-- Panel -->
        {{#> drawer-panel
          drawer-panel--NoBody="true"
          progress--modifier="pf-m-sm"
          }}

          <!-- Content header -->
          {{> master-detail-panel-header
            master-detail-panel-header--title="Node 2"
            master-detail-panel-header--sub-title='<a href="#">siemur/test-space</a>'
            }}

          <!-- Tabs -->
          {{#> drawer-body drawer-body--modifier="pf-m-no-padding"}}
            {{> master-detail-panel-tabs}}
          {{/drawer-body}}

          <!-- Tab content -->
          {{#> drawer-body}}
            {{> master-detail-panel-tab-content}}
          {{/drawer-body}}
        {{/drawer-panel}}
      {{/drawer-main}}
    {{/drawer}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Master-detail-content-body-padding isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-panel-body-padding"
  master-detail-template--title="Padded content example"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  }}

  {{#> divider divider--type="div"}}{{/divider}}
  {{#> page-main-section page-main-section--modifier="pf-m-no-padding"}}

    <!-- Drawer -->
    {{#> drawer
      drawer--id=(concat master-detail-template--id '-drawer')
      drawer--modifier="pf-m-inline-on-2xl"
      drawer-panel--IsOpen="true"
      }}

      {{#> drawer-main}}

        <!-- Content -->
        {{#> drawer-content drawer-content--modifier="pf-m-no-background" drawer-body--modifier="pf-m-padding"}}
          {{> master-detail-toolbar}}
          {{> master-detail-data-list}}
        {{/drawer-content}}

        <!-- Panel -->
        {{#> drawer-panel
          drawer-panel--NoBody="true"
          progress--modifier="pf-m-sm"
          }}

          <!-- Panel header -->
          {{> master-detail-panel-header
            master-detail-panel-header--title="Patternfly-elements"
            master-detail-panel-header--sub-title="PatternFly elements"
            }}

          <!-- Tab content -->
          {{#> drawer-body}}
            {{> master-detail-panel-body}}
          {{/drawer-body}}
        {{/drawer-panel}}

      {{/drawer-main}}
    {{/drawer}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Master-detail-card-view-expanded isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-card-view-expanded-example"
  master-detail-template--title="Main title"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  }}

  {{#> page-main-section page-main-section--modifier="pf-m-no-padding"}}

    <!-- Drawer -->
    {{#> drawer
      drawer--id=(concat master-detail-template--id '-drawer')
      drawer--modifier="pf-m-inline-on-2xl"
      drawer-panel--IsOpen="true"
      }}

      {{#> drawer-section}}
        {{> master-detail-toolbar master-detail-toolbar--HasViewToggle="true"}}
        {{#> divider divider--type="div"}}{{/divider}}
      {{/drawer-section}}

      {{#> drawer-main}}

        <!-- Content -->
        {{#> drawer-content drawer-content--modifier="pf-m-no-background" drawer-body--modifier="pf-m-padding"}}
          {{> master-detail-card-view}}
        {{/drawer-content}}

        <!-- Panel -->
        {{#> drawer-panel
          drawer-panel--modifier=""
          drawer-panel--NoBody="true"
          }}

          <!-- Panel header -->
          {{> master-detail-panel-header
            master-detail-panel-header--title="Patternfly"
            master-detail-panel-header--sub-title="PatternFly elements"
            }}

          {{#> drawer-body}}
            {{> master-detail-panel-body}}
          {{/drawer-body}}

        {{/drawer-panel}}
      {{/drawer-main}}
    {{/drawer}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Master-detail-card-simple-list-expanded-on-mobile isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-card-simple-list-example"
  master-detail-template--title="Master detail, in card, simple list"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  }}

  {{#> page-main-section}}
    {{#> card}}

      <!-- Drawer -->
      {{#> drawer
        drawer--id=(concat master-detail-template--id '-drawer')
        drawer--IsStatic="true"
        drawer-panel--IsOpen="true"
        }}

        {{#> drawer-main}}
          <!-- Content -->
          {{#> drawer-content drawer-content--NoBody="true"}}
            {{#> drawer-content-body drawer-content-body--modifier="pf-m-no-padding"}}
              {{> master-detail-simple-list}}
            {{/drawer-content-body}}
          {{/drawer-content}}

          <!-- Panel -->
          {{#> drawer-panel
            drawer-panel--type="div"
            drawer-panel--attribute=(concat 'id="' master-detail-template--id '-panel" aria-label="Panel"')
            drawer-panel--modifier="pf-m-width-75-on-xl"
            drawer-panel--NoBody="true"
            }}

            <!-- Panel header -->
            {{> master-detail-panel-header
              master-detail-panel-header--title="Patternfly-elements"
              }}

            {{#> drawer-body}}
              {{> master-detail-panel-body}}
            {{/drawer-body}}
          {{/drawer-panel}}
        {{/drawer-main}}
      {{/drawer}}
    {{/card}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Master-detail-card-data-list-expanded-on-mobile isFullscreen
{{#> master-detail-template
  master-detail-template--id="master-detail-card-data-list-example"
  master-detail-template--title="Master detail, in card, data list"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  }}

  {{#> page-main-section}}
    {{#> card}}

      <!-- Drawer -->
      {{#> drawer
        drawer--id=(concat master-detail-template--id '-drawer')
        drawer--IsStatic="true"
        drawer-panel--IsOpen="true"
        }}

        {{#> drawer-main}}

          <!-- Content -->
          {{#> drawer-content}}
            {{#> drawer-body}}
              {{> master-detail-card-toolbar}}
              {{> master-detail-card-data-list}}
            {{/drawer-body}}
          {{/drawer-content}}

          <!-- Panel -->
          {{#> drawer-panel
            drawer-panel--type="div"
            drawer-panel--attribute=(concat 'id="' master-detail-template--id '-panel" aria-label="Panel"')
            drawer-panel--modifier="pf-m-width-75-on-2xl"
            drawer-panel--NoBody="true"
            }}

            <!-- Panel header -->
            {{> master-detail-panel-header
              master-detail-panel-header--title="Patternfly-elements"
              }}

            {{#> drawer-body}}
              {{> master-detail-panel-body}}
            {{/drawer-body}}
          {{/drawer-panel}}
        {{/drawer-main}}
      {{/drawer}}
    {{/card}}
  {{/page-main-section}}
{{/master-detail-template}}
```

```hbs title=Inline-modifier isFullscreen
{{#> master-detail-template
  master-detail-template--id="independent-scroll-example"
  master-detail-template--title="Master detail expanded, with data-list and .pf-m-inline modifier demo"
  master-detail-template--description="Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5"
  master-detail-template--modifier="pf-m-light pf-m-inline"
  }}
  {{#> divider divider--type="div"}}{{/divider}}
  <!-- Drawer -->
  {{#> drawer
    drawer--id=(concat master-detail-template--id '-drawer')
    drawer-panel--IsOpen="true"
    drawer--modifier="pf-m-inline-on-2xl"
    }}

    {{#> drawer-main}}

      <!-- Content -->
      {{#> drawer-content}}
        {{#> drawer-body}}
          {{> master-detail-toolbar}}
          {{> master-detail-data-list}}
        {{/drawer-body}}
      {{/drawer-content}}

      <!-- Panel -->
      {{#> drawer-panel
        drawer-panel--type="div"
        drawer-panel--NoBody="true"
        progress--modifier="pf-m-sm"
        }}

        <!-- Panel header -->
        {{> master-detail-panel-header
          master-detail-panel-header--title="Node 2"
          master-detail-panel-header--sub-title='<a href="#">siemur/test-space</a>'
          }}

        <!-- Tabs -->
        {{#> drawer-body drawer-body--modifier="pf-m-no-padding"}}
          {{> master-detail-panel-tabs}}
        {{/drawer-body}}

        <!-- Tab content -->
        {{#> drawer-body}}
          {{> master-detail-panel-tab-content}}
        {{/drawer-body}}
      {{/drawer-panel}}
    {{/drawer-main}}
  {{/drawer}}
{{/master-detail-template}}
```

## Documentation

This demo implements the drawer in context of the page component.
