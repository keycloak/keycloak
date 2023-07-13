const DATE_AND_TIME_FORMAT: Intl.DateTimeFormatOptions = {
  dateStyle: "long",
  timeStyle: "short",
};

//todo use user local
export default function useFormatDate() {
  return function formatDate(
    date: Date,
    options: Intl.DateTimeFormatOptions = DATE_AND_TIME_FORMAT,
  ) {
    return date.toLocaleString("en", options);
  };
}
