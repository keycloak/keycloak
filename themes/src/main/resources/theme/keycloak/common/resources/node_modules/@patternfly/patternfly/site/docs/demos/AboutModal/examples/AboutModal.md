---
title: About modal
section: demos
---

## Demos
```hbs title=Basic isFullscreen
{{#> about-modal}}
  {{#> backdrop}}
    {{#> bullseye}}
      {{#> about-modal-box about-modal-box--attribute='aria-labelledby="about-modal-title"'}}
        {{#> about-modal-box-brand}}
          {{#> about-modal-box-brand-image about-modal-box-brand-image--attribute='src="/assets/images/pf_mini_logo_white.svg" alt="PatternFly brand logo"'}}
          {{/about-modal-box-brand-image}}
        {{/about-modal-box-brand}}
        {{#> about-modal-box-close}}
          {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
            <i class="fas fa-times" aria-hidden="true"></i>
          {{/button}}
        {{/about-modal-box-close}}
        {{#> about-modal-box-header}}
          {{#> title titleType="h1" title--modifier="pf-m-4xl" title--attribute='id="about-modal-title"'}}
              Red Hat OpenShift Container Platform
          {{/title}}
        {{/about-modal-box-header}}
        {{#> about-modal-box-hero}}
        {{/about-modal-box-hero}}
        {{#> about-modal-box-content}}
          {{#> content}}
          <dl>
            <dt>CFME version</dt>
            <dd>5.5.3.4.20102789036450</dd>
            <dt>Cloudforms version</dt>
            <dd>4.1</dd>
            <dt>Server name</dt>
            <dd>40DemoMaster</dd>
            <dt>User name</dt>
            <dd>Administrator</dd>
            <dt>User role</dt>
            <dd>EvmRole-super_administrator</dd>
            <dt>Browser version</dt>
            <dd>601.2</dd>
            <dt>Browser OS</dt>
            <dd>Mac</dd>
          </dl>
          {{/content}}
          {{#> about-modal-box-strapline}}
            Trademark and copyright information here
          {{/about-modal-box-strapline}}
        {{/about-modal-box-content}}
      {{/about-modal-box}}
    {{/bullseye}}
  {{/backdrop}}
{{/about-modal}}
```

## Documentation
This demo implements the about modal, including the backdrop.
