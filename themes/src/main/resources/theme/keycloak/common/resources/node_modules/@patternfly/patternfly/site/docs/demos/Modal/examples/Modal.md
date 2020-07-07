---
title: Modal
section: demos
---

## Demos
```hbs title=Basic isFullscreen
{{#> modal}}
  {{#> backdrop}}
    {{#> bullseye}}
      {{#> modal-box modal-box--modifier="pf-m-sm" modal-box--attribute='aria-labelledby="modal-title" aria-describedby="modal-description"'}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
        {{#> modal-box-header}}
          {{#> modal-box-title modal-box-title--attribute='id="modal-title"'}}
            Overwrite existing file?
          {{/modal-box-title}}
        {{/modal-box-header}}
        {{#> modal-box-body modal-box-body--attribute='id="modal-description"'}}
          <p>general_modal_final_finalfinal_v9_actualfinal.sketch</p>
          <p>A file with this name already exists, would you like to overwrite the existing file or save a new copy?</p>
        {{/modal-box-body}}
        {{#> modal-box-footer}}
          {{#> button button--modifier="pf-m-primary"}}
            Overwrite
          {{/button}}
          {{#> button button--modifier="pf-m-link"}}
            Save a copy
          {{/button}}
        {{/modal-box-footer}}
      {{/modal-box}}
    {{/bullseye}}
  {{/backdrop}}
{{/modal}}
```

```hbs title=Scrollable-content isFullscreen
{{#> modal}}
  {{#> backdrop}}
    {{#> bullseye}}
      {{#> modal-box modal-box--modifier="pf-m-sm" modal-box--attribute='aria-labelledby="modal-scroll-title" aria-describedby="modal-scroll-description"'}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
        {{#> modal-box-header}}
          {{#> modal-box-title  modal-box-title--attribute='id="modal-scroll-title"'}}
            This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.
          {{/modal-box-title}}
          {{#> modal-box-description modal-box-description--attribute='id="modal-scroll-description"'}}
            This is a modal description. The description will not scroll with the body contents.
          {{/modal-box-description}}
        {{/modal-box-header}}
        {{#> modal-box-body}}
          <p>general_modal_final_finalfinal_v9_actualfinal.sketch</p>
          <p>A file with this name already exists, would you like to overwrite the existing file or save a new copy?</p>
          <p>Curabitur ligula sapien, tincidunt non, euismod vitae, posuere imperdiet, leo. Integer tincidunt. Integer tincidunt. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.</p>
          <p>Duis leo. Praesent blandit laoreet nibh. Ut a nisl id ante tempus hendrerit. Maecenas nec odio et ante tincidunt tempus.
          Ut a nisl id ante tempus hendrerit. Nulla sit amet est. Suspendisse nisl elit, rhoncus eget, elementum ac, condimentum eget, diam. Praesent turpis. Phasellus accumsan cursus velit. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Cras ultricies mi eu turpis hendrerit fringilla. Praesent porttitor, nulla vitae posuere iaculis, arcu nisl dignissim dolor, a pretium mi sem ut ipsum.</p>
          <p>Etiam sit amet orci eget eros faucibus tincidunt. Aliquam eu nunc. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Nunc nec neque.</p>
          <p>Ut a nisl id ante tempus hendrerit. Nulla sit amet est. Suspendisse nisl elit, rhoncus eget, elementum ac, condimentum eget, diam. Praesent turpis. Phasellus accumsan cursus velit. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Cras ultricies mi eu turpis hendrerit fringilla. Praesent porttitor, nulla vitae posuere iaculis, arcu nisl dignissim dolor, a pretium mi sem ut ipsum.</p>
          <p>Etiam sit amet orci eget eros faucibus tincidunt. Aliquam eu nunc. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Nunc nec neque.
          </p>
        {{/modal-box-body}}
        {{#> modal-box-footer}}
          {{#> button button--modifier="pf-m-primary"}}
            Overwrite
          {{/button}}
          {{#> button button--modifier="pf-m-link"}}
            Save a copy
          {{/button}}
        {{/modal-box-footer}}
      {{/modal-box}}
    {{/bullseye}}
  {{/backdrop}}
{{/modal}}
```

```hbs title=Medium isFullscreen
{{#> modal}}
  {{#> backdrop}}
    {{#> bullseye}}
      {{#> modal-box modal-box--modifier="pf-m-md" modal-box--attribute='aria-labelledby="modal-md-title" aria-describedby="modal-md-description"'}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
        {{#> modal-box-header}}
          {{#> modal-box-title modal-box-title--attribute='id="modal-md-title"'}}
            This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.
          {{/modal-box-title}}
        {{/modal-box-header}}
        {{#> modal-box-body}}
          <p id="modal-md-description">The "aria-describedby" attribute can be applied to any text that adequately describes the modal's purpose. It does not have to be assigned to ".pf-c-modal-box__body"</p>
          <p>Form here</p>
        {{/modal-box-body}}
        {{#> modal-box-footer}}
          {{#> button button--modifier="pf-m-primary"}}
            Save
          {{/button}}
          {{#> button button--modifier="pf-m-link"}}
            Cancel
          {{/button}}
        {{/modal-box-footer}}
      {{/modal-box}}
    {{/bullseye}}
  {{/backdrop}}
{{/modal}}
```

```hbs title=Large isFullscreen
{{#> modal}}
  {{#> backdrop}}
    {{#> bullseye}}
      {{#> modal-box modal-box--modifier="pf-m-lg" modal-box--attribute='aria-labelledby="modal-lg-title" aria-describedby="modal-lg-description"'}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
        {{#> modal-box-header}}
          {{#> modal-box-title modal-box-title--attribute='id="modal-lg-title"'}}
            This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.
          {{/modal-box-title}}
        {{/modal-box-header}}
        {{#> modal-box-body}}
          <p id="modal-lg-description">The "aria-describedby" attribute can be applied to any text that adequately describes the modal's purpose. It does not have to be assigned to ".pf-c-modal-box__body"</p>
          <p>Form here</p>
        {{/modal-box-body}}
        {{#> modal-box-footer}}
          {{#> button button--modifier="pf-m-primary"}}
            Save
          {{/button}}
          {{#> button button--modifier="pf-m-link"}}
            Cancel
          {{/button}}
        {{/modal-box-footer}}
      {{/modal-box}}
    {{/bullseye}}
  {{/backdrop}}
{{/modal}}
```
