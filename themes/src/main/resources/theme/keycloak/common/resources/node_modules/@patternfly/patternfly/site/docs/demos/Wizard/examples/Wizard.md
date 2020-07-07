---
title: Wizard
section: demos
wrapperTag: div
---

## Demos
```hbs title=Basic isFullscreen
{{#> backdrop}}
  {{#> bullseye}}
    {{#> modal-box modal-box--modifier="pf-m-lg" modal-box--attribute='aria-label="Basic wizard"'}}
      {{#> wizard}}
        {{#> wizard-header}}
          {{#> button button--modifier="pf-m-plain pf-c-wizard__close" button--attribute='aria-label="Close"'}}
            <i class="fas fa-times" aria-hidden="true"></i>
          {{/button}}
          {{#> title title--modifier="pf-m-3xl pf-c-wizard__title"}}Wizard title{{/title}}
          {{#> wizard-description}}
            Here is where the description goes
          {{/wizard-description}}
        {{/wizard-header}}
        {{#> wizard-toggle}}
          {{#> wizard-toggle-list}}
              {{#> wizard-toggle-list-item}}
                {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
                Configuration
                {{> wizard-toggle-separator}}
              {{/wizard-toggle-list-item}}
              {{#> wizard-toggle-list-item}}
                Substep B
              {{/wizard-toggle-list-item}}
            {{/wizard-toggle-list}}
            {{> wizard-toggle-icon}}
          {{/wizard-toggle}}
          {{#> wizard-outer-wrap}}
            {{#> wizard-inner-wrap}}
              {{#> wizard-nav}}
                {{#> wizard-nav-list}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Information
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
                      Configuration
                    {{/wizard-nav-link}}
                    {{#> wizard-nav-list}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep A
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                          Substep B
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep C
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                    {{/wizard-nav-list}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Additional
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-disabled" wizard-nav-link--attribute='aria-disabled="true" tabindex="-1"'}}
                      Review
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                {{/wizard-nav-list}}
              {{/wizard-nav}}
            {{#> wizard-main}}
              <p>Wizard content goes here</p>
            {{/wizard-main}}
          {{/wizard-inner-wrap}}
          {{#> wizard-footer}}
            {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
              Next
            {{/button}}
            {{#> button button--modifier="pf-m-secondary"}}
              Back
            {{/button}}
            {{#> button button--modifier="pf-m-link"}}
              Cancel
            {{/button}}
          {{/wizard-footer}}
        {{/wizard-outer-wrap}}
      {{/wizard}}
    {{/modal-box}}
  {{/bullseye}}
{{/backdrop}}
```

```hbs title=Nav-expanded-(mobile) isFullscreen
{{#> backdrop}}
  {{#> bullseye}}
    {{#> modal-box modal-box--modifier="pf-m-lg" modal-box--attribute='aria-label="Wizard with expanded mobile nav"'}}
      {{#> wizard wizard--IsExpanded="true"}}
        {{#> wizard-header}}
          {{#> button button--modifier="pf-m-plain pf-c-wizard__close" button--attribute='aria-label="Close"'}}
            <i class="fas fa-times" aria-hidden="true"></i>
          {{/button}}
          {{#> title title--modifier="pf-m-3xl pf-c-wizard__title"}}Wizard title{{/title}}
          {{#> wizard-description}}
            Here is where the description goes
          {{/wizard-description}}
        {{/wizard-header}}
        {{#> wizard-toggle}}
          {{#> wizard-toggle-list}}
            {{#> wizard-toggle-list-item}}
              {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
              Configuration
              {{> wizard-toggle-separator}}
            {{/wizard-toggle-list-item}}
            {{#> wizard-toggle-list-item}}
              Substep B
            {{/wizard-toggle-list-item}}
          {{/wizard-toggle-list}}
          {{> wizard-toggle-icon}}
        {{/wizard-toggle}}
        {{#> wizard-outer-wrap}}
          {{#> wizard-inner-wrap}}
            {{#> wizard-nav}}
              {{#> wizard-nav-list}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link}}
                    Information
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
                    Configuration
                  {{/wizard-nav-link}}
                  {{#> wizard-nav-list}}
                    {{#> wizard-nav-item}}
                      {{#> wizard-nav-link}}
                        Substep A
                      {{/wizard-nav-link}}
                    {{/wizard-nav-item}}
                    {{#> wizard-nav-item}}
                      {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                        Substep B
                      {{/wizard-nav-link}}
                    {{/wizard-nav-item}}
                    {{#> wizard-nav-item}}
                      {{#> wizard-nav-link}}
                        Substep C
                      {{/wizard-nav-link}}
                    {{/wizard-nav-item}}
                  {{/wizard-nav-list}}
                {{/wizard-nav-item}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link}}
                    Additional
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-disabled" wizard-nav-link--attribute='aria-disabled="true" tabindex="-1"'}}
                    Review
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
              {{/wizard-nav-list}}
            {{/wizard-nav}}
            {{#> wizard-main}}
              <p>Wizard content goes here</p>
            {{/wizard-main}}
          {{/wizard-inner-wrap}}
          {{#> wizard-footer}}
            {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
              Next
            {{/button}}
            {{#> button button--modifier="pf-m-secondary"}}
              Back
            {{/button}}
            {{#> button button--modifier="pf-m-link"}}
              Cancel
            {{/button}}
          {{/wizard-footer}}
        {{/wizard-outer-wrap}}
      {{/wizard}}
    {{/modal-box}}
  {{/bullseye}}
{{/backdrop}}
```

```hbs title=In-page isFullscreen
{{#> page page--id="wizard-in-page"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{!-- Brand --}}
    {{#> page-header-brand}}
      {{#> page-header-brand-toggle}}
        {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' page--id '-nav-toggle" aria-label="Global navigation" aria-expanded="true" aria-controls="' page--id '-primary-nav"')}}
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
  {{!-- Nav --}}
  {{#> page-sidebar}}
    {{#> nav nav--attribute=(concat 'id="' page--id '-primary-nav" aria-label="Global"')}}
      {{#> nav-list}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#" nav-link--current="true"}}
            System panel
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Policy
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Authentication
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Network services
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Server
          {{/nav-link}}
        {{/nav-item}}
      {{/nav-list}}
    {{/nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-template-breadcrumb}}
    {{/page-template-breadcrumb}}
    {{#> page-template-title}}
    {{/page-template-title}}
    {{#> page-main-wizard}}
      {{#> wizard}}
        {{#> wizard-toggle}}
          {{#> wizard-toggle-list}}
              {{#> wizard-toggle-list-item}}
                {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
                Configuration
                {{> wizard-toggle-separator}}
              {{/wizard-toggle-list-item}}
              {{#> wizard-toggle-list-item}}
                Substep B
              {{/wizard-toggle-list-item}}
            {{/wizard-toggle-list}}
            {{> wizard-toggle-icon}}
          {{/wizard-toggle}}
          {{#> wizard-outer-wrap}}
            {{#> wizard-inner-wrap}}
              {{#> wizard-nav}}
                {{#> wizard-nav-list}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Information
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
                      Configuration
                    {{/wizard-nav-link}}
                    {{#> wizard-nav-list}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep A
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                          Substep B
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep C
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                    {{/wizard-nav-list}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Additional
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-disabled" wizard-nav-link--attribute='aria-disabled="true" tabindex="-1"'}}
                      Review
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                {{/wizard-nav-list}}
              {{/wizard-nav}}
            {{#> wizard-main wizard-main--type="div"}}
              <p>Wizard content goes here</p>
            {{/wizard-main}}
          {{/wizard-inner-wrap}}
          {{#> wizard-footer}}
            {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
              Next
            {{/button}}
            {{#> button button--modifier="pf-m-secondary"}}
              Back
            {{/button}}
            {{#> button button--modifier="pf-m-link"}}
              Cancel
            {{/button}}
          {{/wizard-footer}}
        {{/wizard-outer-wrap}}
      {{/wizard}}
    {{/page-main-wizard}}
  {{/page-main}}
{{/page}}
```

```hbs title=In-page-nav-expanded-(mobile) isFullscreen
{{#> page page--id="wizard-in-page-expanded"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{!-- Brand --}}
    {{#> page-header-brand}}
      {{#> page-header-brand-toggle}}
        {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' page--id '-nav-toggle" aria-label="Global navigation" aria-expanded="true" aria-controls="' page--id '-primary-nav"')}}
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
  {{!-- Nav --}}
  {{#> page-sidebar}}
    {{#> nav nav--attribute=(concat 'id="' page--id '-primary-nav" aria-label="Global"')}}
      {{#> nav-list}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#" nav-link--current="true"}}
            System panel
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Policy
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Authentication
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Network services
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Server
          {{/nav-link}}
        {{/nav-item}}
      {{/nav-list}}
    {{/nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-template-breadcrumb}}
    {{/page-template-breadcrumb}}
    {{#> page-template-title}}
    {{/page-template-title}}
    {{#> page-main-wizard}}
      {{#> wizard wizard--IsExpanded="true"}}
        {{#> wizard-toggle}}
          {{#> wizard-toggle-list}}
              {{#> wizard-toggle-list-item}}
                {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
                Configuration
                {{> wizard-toggle-separator}}
              {{/wizard-toggle-list-item}}
              {{#> wizard-toggle-list-item}}
                Substep B
              {{/wizard-toggle-list-item}}
            {{/wizard-toggle-list}}
            {{> wizard-toggle-icon}}
          {{/wizard-toggle}}
          {{#> wizard-outer-wrap}}
            {{#> wizard-inner-wrap}}
              {{#> wizard-nav}}
                {{#> wizard-nav-list}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Information
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
                      Configuration
                    {{/wizard-nav-link}}
                    {{#> wizard-nav-list}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep A
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                          Substep B
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                      {{#> wizard-nav-item}}
                        {{#> wizard-nav-link}}
                          Substep C
                        {{/wizard-nav-link}}
                      {{/wizard-nav-item}}
                    {{/wizard-nav-list}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link}}
                      Additional
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                  {{#> wizard-nav-item}}
                    {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-disabled" wizard-nav-link--attribute='aria-disabled="true" tabindex="-1"'}}
                      Review
                    {{/wizard-nav-link}}
                  {{/wizard-nav-item}}
                {{/wizard-nav-list}}
              {{/wizard-nav}}
            {{#> wizard-main wizard-main--type="div"}}
              <p>Wizard content goes here</p>
            {{/wizard-main}}
          {{/wizard-inner-wrap}}
          {{#> wizard-footer}}
            {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
              Next
            {{/button}}
            {{#> button button--modifier="pf-m-secondary"}}
              Back
            {{/button}}
            {{#> button button--modifier="pf-m-link"}}
              Cancel
            {{/button}}
          {{/wizard-footer}}
        {{/wizard-outer-wrap}}
      {{/wizard}}
    {{/page-main-wizard}}
  {{/page-main}}
{{/page}}
```
