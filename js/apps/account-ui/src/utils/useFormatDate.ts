const DATE_AND_TIME_FORMAT: Intl.DateTimeFormatOptions = {
  dateStyle: "long",
  timeStyle: "short",
};

//todo use user local
export default function useFormatDate() {
  return {
    formatDate: function (
      date: Date,
      options: Intl.DateTimeFormatOptions | undefined = DATE_AND_TIME_FORMAT,
    ) {
      return date.toLocaleString("en", options);
    },
  };
}
