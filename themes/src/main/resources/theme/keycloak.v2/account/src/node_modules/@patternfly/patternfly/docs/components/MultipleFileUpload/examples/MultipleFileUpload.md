---
id: 'File upload - multiple'
beta: true
section: components
cssPrefix: pf-c-multiple-file-upload
---## Examples

### Basic

```html
<div class="pf-c-multiple-file-upload">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
</div>

```

### Drag over

```html
<div class="pf-c-multiple-file-upload pf-m-drag-over">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
</div>

```

### Horizontal

```html
<div class="pf-c-multiple-file-upload pf-m-horizontal">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
</div>

```

### Horizontal drag over

```html
<div class="pf-c-multiple-file-upload pf-m-horizontal pf-m-drag-over">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
</div>

```

### File upload status

```html
<div class="pf-c-multiple-file-upload">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
  <div class="pf-c-multiple-file-upload__status">
    <div class="pf-c-expandable-section">
      <button
        type="button"
        class="pf-c-expandable-section__toggle"
        aria-expanded="false"
      >
        <span class="pf-c-expandable-section__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span class="pf-c-expandable-section__toggle-text">
          <div class="pf-c-multiple-file-upload__status-progress">
            <div class="pf-c-multiple-file-upload__status-progress-icon">
              <i class="pf-icon-in-progress"></i>
            </div>
            <div
              class="pf-c-multiple-file-upload__status-progress-text"
            >0 of 3 files uploaded</div>
          </div>
        </span>
      </button>
      <div class="pf-c-expandable-section__content" hidden>
        <ul class="pf-c-multiple-file-upload__status-list">
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-png">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-png-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.png</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >40 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">35%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="35"
                  aria-labelledby="multiple-file-upload-progress-png-description"
                >
                  <div class="pf-c-progress__indicator" style="width:35%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-doc">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-doc-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.doc</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >30 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">70%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="70"
                  aria-labelledby="multiple-file-upload-progress-doc-description"
                >
                  <div class="pf-c-progress__indicator" style="width:70%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-pdf">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-pdf-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.pdf</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >25 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">50%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="50"
                  aria-labelledby="multiple-file-upload-progress-pdf-description"
                >
                  <div class="pf-c-progress__indicator" style="width:50%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</div>

```

### File upload status expanded

```html
<div class="pf-c-multiple-file-upload">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
  <div class="pf-c-multiple-file-upload__status">
    <div class="pf-c-expandable-section pf-m-expanded">
      <button
        type="button"
        class="pf-c-expandable-section__toggle"
        aria-expanded="true"
      >
        <span class="pf-c-expandable-section__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span class="pf-c-expandable-section__toggle-text">
          <div class="pf-c-multiple-file-upload__status-progress">
            <div class="pf-c-multiple-file-upload__status-progress-icon">
              <i class="pf-icon-in-progress"></i>
            </div>
            <div
              class="pf-c-multiple-file-upload__status-progress-text"
            >0 of 3 files uploaded</div>
          </div>
        </span>
      </button>
      <div class="pf-c-expandable-section__content">
        <ul class="pf-c-multiple-file-upload__status-list">
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-png">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-png-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.png</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >40 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">35%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="35"
                  aria-labelledby="multiple-file-upload-progress-png-description"
                >
                  <div class="pf-c-progress__indicator" style="width:35%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-doc">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-doc-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.doc</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >30 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">70%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="70"
                  aria-labelledby="multiple-file-upload-progress-doc-description"
                >
                  <div class="pf-c-progress__indicator" style="width:70%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-pdf">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-pdf-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.pdf</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >25 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">50%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="50"
                  aria-labelledby="multiple-file-upload-progress-pdf-description"
                >
                  <div class="pf-c-progress__indicator" style="width:50%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</div>

```

### Horizontal file upload status expanded

```html
<div class="pf-c-multiple-file-upload pf-m-horizontal">
  <div class="pf-c-multiple-file-upload__main">
    <div class="pf-c-multiple-file-upload__title">
      <div class="pf-c-multiple-file-upload__title-icon">
        <i class="fas fa-upload" aria-hidden="true"></i>
      </div>
      <div class="pf-c-multiple-file-upload__title-text">
        Drag and drop files here
        <div class="pf-c-multiple-file-upload__title-text-separator">or</div>
      </div>
    </div>
    <div class="pf-c-multiple-file-upload__upload">
      <button class="pf-c-button pf-m-secondary" type="button">Upload</button>
    </div>
    <div
      class="pf-c-multiple-file-upload__info"
    >Accepted file types: JPEG, Doc, PDF, PNG</div>
  </div>
  <div class="pf-c-multiple-file-upload__status">
    <div class="pf-c-expandable-section pf-m-expanded">
      <button
        type="button"
        class="pf-c-expandable-section__toggle"
        aria-expanded="true"
      >
        <span class="pf-c-expandable-section__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span class="pf-c-expandable-section__toggle-text">
          <div class="pf-c-multiple-file-upload__status-progress">
            <div class="pf-c-multiple-file-upload__status-progress-icon">
              <i class="pf-icon-in-progress"></i>
            </div>
            <div
              class="pf-c-multiple-file-upload__status-progress-text"
            >0 of 3 files uploaded</div>
          </div>
        </span>
      </button>
      <div class="pf-c-expandable-section__content">
        <ul class="pf-c-multiple-file-upload__status-list">
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-png">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-png-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.png</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >40 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">35%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="35"
                  aria-labelledby="multiple-file-upload-progress-png-description"
                >
                  <div class="pf-c-progress__indicator" style="width:35%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-doc">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-doc-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.doc</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >30 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">70%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="70"
                  aria-labelledby="multiple-file-upload-progress-doc-description"
                >
                  <div class="pf-c-progress__indicator" style="width:70%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-multiple-file-upload__status-item">
            <div class="pf-c-multiple-file-upload__status-item-icon">
              <i class="fas fa-file" aria-hidden="true"></i>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-main">
              <div class="pf-c-progress" id="multiple-file-upload-progress-pdf">
                <div
                  class="pf-c-progress__description"
                  id="multiple-file-upload-progress-pdf-description"
                >
                  <span class="pf-c-multiple-file-upload__status-item-progress">
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-text"
                    >filename.pdf</span>
                    <span
                      class="pf-c-multiple-file-upload__status-item-progress-size"
                    >25 mb</span>
                  </span>
                </div>
                <div class="pf-c-progress__status" aria-hidden="true">
                  <span class="pf-c-progress__measure">50%</span>
                </div>
                <div
                  class="pf-c-progress__bar"
                  role="progressbar"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-valuenow="50"
                  aria-labelledby="multiple-file-upload-progress-pdf-description"
                >
                  <div class="pf-c-progress__indicator" style="width:50%;"></div>
                </div>
              </div>
            </div>
            <div class="pf-c-multiple-file-upload__status-item-close">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Remove from list"
              >
                <i class="fas fa-times-circle" aria-hidden="true"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</div>

```

## Documentation

### Usage

| Class                                                   | Applied | Outcome                                                    |
| ------------------------------------------------------- | ------- | ---------------------------------------------------------- |
| `.pf-c-multiple-file-upload`                            | `<div>` | Initiates the multiple file upload component. **Required** |
| `.pf-c-multiple-file-upload__main`                      | `<div>` | Defines the main section.                                  |
| `.pf-c-multiple-file-upload__title`                     | `<div>` | Defines the title.                                         |
| `.pf-c-multiple-file-upload__title-icon`                | `<div>` | Defines the title icon.                                    |
| `.pf-c-multiple-file-upload__title-text`                | `<div>` | Defines the title text.                                    |
| `.pf-c-multiple-file-upload__title-text-separator`      | `<div>` | Defines the title text separator.                          |
| `.pf-c-multiple-file-upload__upload`                    | `<div>` | Defines the upload button.                                 |
| `.pf-c-multiple-file-upload__info`                      | `<div>` | Defines the info section.                                  |
| `.pf-c-multiple-file-upload__status`                    | `<div>` | Defines the status section.                                |
| `.pf-c-multiple-file-upload__status-progress`           | `<div>` | Defines the status toggle progress.                        |
| `.pf-c-multiple-file-upload__status-progress-icon`      | `<div>` | Defines the status toggle progress icon.                   |
| `.pf-c-multiple-file-upload__status-progress-text`      | `<div>` | Defines the status toggle progress text.                   |
| `.pf-c-multiple-file-upload__status-list`               | `<div>` | Defines the status item list.                              |
| `.pf-c-multiple-file-upload__status-item`               | `<div>` | Defines a status item.                                     |
| `.pf-c-multiple-file-upload__status-item-icon`          | `<div>` | Defines the status item icon.                              |
| `.pf-c-multiple-file-upload__status-item-main`          | `<div>` | Defines the status item main progress section.             |
| `.pf-c-multiple-file-upload__status-item-close`         | `<div>` | Defines the status item close button.                      |
| `.pf-c-multiple-file-upload__status-item-progress`      | `<div>` | Defines the status item progress description.              |
| `.pf-c-multiple-file-upload__status-item-progress-text` | `<div>` | Defines the status item progress description text.         |
| `.pf-c-multiple-file-upload__status-item-progress-size` | `<div>` | Defines the status item progress description size.         |
