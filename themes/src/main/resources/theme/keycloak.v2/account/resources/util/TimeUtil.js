function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Stan Silvert
 */
class TimeUtil {
  constructor() {
    _defineProperty(this, "options", {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric'
    });

    _defineProperty(this, "formatter", void 0);

    try {
      this.formatter = new Intl.DateTimeFormat(locale, this.options);
    } catch (e) {
      // unknown locale falling back to English
      this.formatter = new Intl.DateTimeFormat('en', this.options);
    }
  }

  format(time) {
    return this.formatter.format(time);
  }

}

const TimeUtilInstance = new TimeUtil();
export default TimeUtilInstance;
//# sourceMappingURL=TimeUtil.js.map