import { i18n } from "../i18n";

export const FORMAT_DATE_ONLY: Intl.DateTimeFormatOptions = {
  dateStyle: "long",
};

export const FORMAT_TIME_ONLY: Intl.DateTimeFormatOptions = {
  timeStyle: "short",
};

export const FORMAT_DATE_AND_TIME: Intl.DateTimeFormatOptions = {
  ...FORMAT_DATE_ONLY,
  ...FORMAT_TIME_ONLY,
};

export function formatDate(date: Date, options = FORMAT_DATE_AND_TIME) {
  return date.toLocaleString(i18n.languages, options);
}
