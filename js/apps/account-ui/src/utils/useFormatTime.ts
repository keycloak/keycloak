const TIME_FORMAT: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
  hour: "numeric",
  minute: "numeric",
};

//todo use user local
export default function useFormatTime() {
  return {
    formatTime: function (
    time: number,
    options: Intl.DateTimeFormatOptions | undefined = TIME_FORMAT,
) {
    return new Intl.DateTimeFormat("en", options).format(time * 1000);
    },
  };
}
