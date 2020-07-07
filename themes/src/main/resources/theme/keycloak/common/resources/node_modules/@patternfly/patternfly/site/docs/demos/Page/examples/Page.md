---
title: Page
section: demos
wrapperTag: div
---

## Demos
```hbs title=Default-nav isFullscreen
{{> page-demo-default page-demo-default--id="page-default-nav-example"}}
```

```hbs title=Expandable-nav isFullscreen
{{#> page-demo-expandable-nav page-demo-expandable--id="page-expandable-nav-example"}}{{/page-demo-expandable-nav}}
```

```hbs title=Horizontal-nav isFullscreen
{{#> page page--id="page-layout-horizontal-nav"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{#> page-header-brand}}
      {{#> page-header-brand-link page-header-brand-link--href="#"}}
        {{#> brand brand--attribute='src="/assets/images/PF-Masthead-Logo.svg" alt="PatternFly logo"'}}{{/brand}}
      {{/page-header-brand-link}}
    {{/page-header-brand}}
    {{#> page-header-nav}}
      {{#> nav nav--IsHorizontal="true" nav--IsScrollable="true" nav--attribute=(concat 'id="' page--id '-horizontal-nav" aria-label="Global"')}}
        {{#> nav-list}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Horizontal nav item 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Horizontal nav item 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Horizontal nav item 3
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Horizontal nav item 4
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Horizontal nav item 5
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav}}
    {{/page-header-nav}}
    {{#> page-template-header-tools-elements}}
    {{/page-template-header-tools-elements}}
  {{/page-header}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-template-breadcrumb}}
    {{/page-template-breadcrumb}}
    {{#> page-template-title}}
    {{/page-template-title}}
    {{#> page-template-gallery}}
    {{/page-template-gallery}}
  {{/page-main}}
{{/page}}
```

```hbs title=Tertiary-nav isFullscreen
{{#> page page--id="page-layout-tertiary-nav"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{!-- Brand --}}
    {{#> page-header-brand}}
      {{#> page-header-brand-toggle}}
        {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' page--id '-nav-toggle" aria-label="Global navigation" aria-expanded="true" aria-controls="' page--id '-tertiary-nav"')}}
          <i class="fas fa-bars" aria-hidden="true"></i>
        {{/button}}
      {{/page-header-brand-toggle}}
      {{#> page-header-brand-link page-header-brand-link--href="#"}}
        {{#> brand brand--attribute='src="/assets/images/PF-Masthead-Logo.svg" alt="PatternFly logo"'}}{{/brand}}
      {{/page-header-brand-link}}
    {{/page-header-brand}}
    {{#> page-template-header-tools-elements}}
    {{/page-template-header-tools-elements}}
  {{/page-header}}

  {{#> page-sidebar}}
    {{#> nav nav--attribute=(concat 'id="' page--id '-tertiary-nav" aria-label="Global"')}}
      {{#> nav-list}}
        {{#> nav-item nav-item--expandable="true" nav-item--expanded="true" nav-item--current="true"}}
          {{#> nav-link nav-link--href="#" nav-link--attribute='id="tertiary-nav-link1"'}}
            System panel
          {{/nav-link}}
          {{#> nav-subnav nav-subnav--attribute='aria-labelledby="tertiary-nav-link1"'}}
            {{#> nav-list}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Overview
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#" nav-link--current="true"}}
                  Resource usage
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Hypervisors
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Instances
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Volumes
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Networks
                {{/nav-link}}
              {{/nav-item}}
            {{/nav-list}}
          {{/nav-subnav}}
        {{/nav-item}}
        {{#> nav-item nav-item--expandable="true"}}
          {{#> nav-link nav-link--href="#" nav-link--attribute='id="tertiary-nav-link2"'}}
            Policy
          {{/nav-link}}
          {{#> nav-subnav nav-subnav--attribute='aria-labelledby="tertiary-nav-link2"'}}
            {{#> nav-list}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Subnav link 1
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Subnav link 2
                {{/nav-link}}
              {{/nav-item}}
            {{/nav-list}}
          {{/nav-subnav}}
        {{/nav-item}}
        {{#> nav-item nav-item--expandable="true"}}
          {{#> nav-link nav-link--href="#" nav-link--attribute='id="tertiary-nav-link3"'}}
            Authentication
          {{/nav-link}}
          {{#> nav-subnav nav-subnav--attribute='aria-labelledby="tertiary-nav-link3"'}}
            {{#> nav-list}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Subnav link 1
                {{/nav-link}}
              {{/nav-item}}
              {{#> nav-item newcontent}}
                {{#> nav-link nav-link--href="#"}}
                  Subnav link 2
                {{/nav-link}}
              {{/nav-item}}
            {{/nav-list}}
          {{/nav-subnav}}
        {{/nav-item}}
      {{/nav-list}}
    {{/nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-main-nav}}
      {{#> nav nav--IsHorizontal="true" nav--IsTertiary="true" nav--IsScrollable="true" nav--attribute='aria-label="Local"'}}
        {{#> nav-list}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Tertiary nav item 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Tertiary nav item 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Tertiary nav item 3
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Tertiary nav item 4
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item}}
            {{#> nav-link nav-link--href="#"}}
              Tertiary nav item 5
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav}}
    {{/page-main-nav}}
    {{#> page-template-breadcrumb}}
    {{/page-template-breadcrumb}}
    {{#> page-template-title}}
    {{/page-template-title}}
    {{#> page-template-gallery}}
    {{/page-template-gallery}}
  {{/page-main}}
{{/page}}
```

```hbs title=Grouped-nav isFullscreen
{{#> page page--id="page-layout-grouped-nav"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{#> page-header-brand}}
      {{#> page-header-brand-toggle}}
        {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' page--id '-nav-toggle" aria-label="Global navigation" aria-expanded="true" aria-controls="' page--id '-grouped-nav"')}}
          <i class="fas fa-bars" aria-hidden="true"></i>
        {{/button}}
      {{/page-header-brand-toggle}}
      {{#> page-header-brand-link page-header-brand-link--href="#"}}
        {{#> brand brand--attribute='src="/assets/images/PF-Masthead-Logo.svg" alt="PatternFly logo"'}}{{/brand}}
      {{/page-header-brand-link}}
    {{/page-header-brand}}
    {{#> page-template-header-tools-elements}}
    {{/page-template-header-tools-elements}}
  {{/page-header}}
  {{#> page-sidebar}}
    {{#> nav nav--attribute=(concat 'id="' page--id '-grouped-nav" aria-label="Global"')}}
      {{#> nav-section nav-section--attribute='aria-labelledby="grouped-title1"'}}
        {{#> nav-section-title nav-section-title--attribute='id="grouped-title1"'}}
          System panel
        {{/nav-section-title}}
        {{#> nav-list nav-list--type="simple"}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Overview
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Resource usage
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Hypervisors
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Instances
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Volumes
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Networks
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-section}}
      {{#> nav-section nav-section--attribute='aria-labelledby="grouped-title2"'}}
        {{#> nav-section-title nav-section-title--attribute='id="grouped-title2"'}}
          Policy
        {{/nav-section-title}}
        {{#> nav-list nav-list--type="simple"}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Hosts
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Virtual machines
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Storage
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-section}}
    {{/nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-template-title}}
    {{/page-template-title}}
    {{#> page-main-section page-main-section--modifier="pf-m-light"}}
    {{/page-main-section}}
    {{#> page-main-section page-main-section--modifier="pf-m-dark-200"}}
    {{/page-main-section}}
    {{#> page-main-section}}
    {{/page-main-section}}
  {{/page-main}}
{{/page}}
```

```hbs title=Light-theme-sidebar-and-nav isFullscreen
{{#> page-demo-expandable-nav page-demo-expandable--id="page-light-sidebar-nav-example"  page-sidebar--modifier="pf-m-light" nav--modifier="pf-m-light"}}{{/page-demo-expandable-nav}}
```

## Documentation
This demo implements all variations of the nav component in the page component.
