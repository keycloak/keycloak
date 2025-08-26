import { useWhoAmI } from "../context/whoami/WhoAmI";

export const FORMAT_DATE_AND_TIME: Intl.DateTimeFormatOptions = {
  dateStyle: "long",
  timeStyle: "short",
};

export default function useFormatDate() {
  const { whoAmI } = useWhoAmI();

  return function formatDate(date: Date, options?: Intl.DateTimeFormatOptions) {
    return date.toLocaleString(whoAmI.locale, options);
  };
}
