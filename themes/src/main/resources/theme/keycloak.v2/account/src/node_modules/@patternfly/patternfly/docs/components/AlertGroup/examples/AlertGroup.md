---
id: Alert group
section: components
cssPrefix: pf-c-alert-group
---## Examples

### Static alert group

```html
<ul class="pf-c-alert-group">
  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-inline pf-m-success" aria-label="Success alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Success alert:</span>
        Success alert title
      </p>
    </div>
  </li>

  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-inline pf-m-danger" aria-label="Danger alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Danger alert:</span>
        Danger alert title
      </p>
    </div>
  </li>

  <li class="pf-c-alert-group__item">
    <div
      class="pf-c-alert pf-m-inline pf-m-info"
      aria-label="Information alert"
    >
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Info alert:</span>
        Info alert title
      </p>
      <div class="pf-c-alert__description">
        <p>
          Info alert description.
          <a href="#">This is a link.</a>
        </p>
      </div>
    </div>
  </li>
</ul>

```

### Overview

`.pf-c-alert-group` is optional when only one alert is needed. It becomes required when more than one alert is used in a list.

### Usage

| Attribute                 | Applied to | Outcome                                        |
| ------------------------- | ---------- | ---------------------------------------------- |
| `.pf-c-alert-group`       | `<ul>`     | Creates an alert group component. **Required** |
| `.pf-c-alert-group__item` | `<li>`     | Creates an alert group item. **Required**      |

### Toast alert group

```html isFullscreen
<ul class="pf-c-alert-group pf-m-toast">
  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-success" aria-label="Success toast alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title" id="alert_one_title">
        <span class="pf-screen-reader">Success alert:</span>
        Success toast alert title
      </p>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close success alert: Success alert title"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </li>

  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-danger" aria-label="Danger toast alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title" id="alert_two_title">
        <span class="pf-screen-reader">Danger alert:</span>
        Danger toast alert title
      </p>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close success alert: Success alert title"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </li>

  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-info" aria-label="Information toast alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title" id="alert_three_title">
        <span class="pf-screen-reader">Info alert:</span>
        Info toast alert title
      </p>
      <div class="pf-c-alert__description">
        <p>
          Info toast alert description. From the settings tab, click
          <a href="#">View logs</a>&nbsp;to review the details.
        </p>
      </div>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close success alert: Success alert title"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </li>

  <li class="pf-c-alert-group__item">
    <button class="pf-c-alert-group__overflow-button">View 3 more notifications</button>
  </li>
</ul>

```

### Overview

An alert group that includes the `.pf-m-toast` modifier becomes a toast alert group with unique positioning in the top-right corner of the window. `.pf-c-alert-group` is required to create a toast alert group.

Every toast alert must include a close button to dismiss the alert.

When toast alerts include a link or action, these elements are not announced as interactive elements by screen readers. Whenever you include a link or button, incorporate it into the message so that it’s clear to the user how to access the same contents or action without clicking the link or button directly in the toast. For example, if your toast alert displays a message “The build is complete. Go to the Builds page to download,” the screen reader user is given instructions on how to find their build for download.

For sighted users, interactive elements can be included in this message in one of the following ways:

-   Using a link to the Builds page: “The build is complete. Go to the [Builds](<>) page to download” using `<a href="url">Builds</a>`
-   Using a button to download: “The build is complete. Go to the Builds page to [download](<>)" using `<button class="pf-c-button pf-m-link pf-m-inline type="button">download</button>`

### Modifiers

| Class                                | Applied to          | Outcome                                                            |
| ------------------------------------ | ------------------- | ------------------------------------------------------------------ |
| `.pf-m-toast`                        | `.pf-c-alert-group` | Applies toast alert styling to an alert group.                     |
| `.pf-c-alert-group__overflow-button` | `<button>`          | Applies overflow button styling to an alert group overflow button. |

## Documentation

### Overview

Alert groups are used to contain and align consecutive alerts. Groups can either be embedded alongside a page's content or in the top-right corner as a toast group using the `.pf-m-toast` modifier.
