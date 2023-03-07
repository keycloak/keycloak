const DATE_AND_TIME_FORMAT: Intl.DateTimeFormatOptions = {
  dateStyle: "long",
  timeStyle: "short",
};

const TIME_FORMAT: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
  hour: "numeric",
  minute: "numeric",
};

export default function useFormatter() {
  return {
    formatDate: function (
      date: Date,
      options: Intl.DateTimeFormatOptions | undefined = DATE_AND_TIME_FORMAT
    ) {
      return date.toLocaleString("en", options);
    },
    formatTime: function (
      time: number,
      options: Intl.DateTimeFormatOptions | undefined = TIME_FORMAT
    ) {
      return new Intl.DateTimeFormat("en", options).format(time);
    },
  };
}
