import type { IFormatter, IFormatterValueType } from "@patternfly/react-table";
import FileSaver from "file-saver";
import _ from "lodash";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { useTranslation } from "react-i18next";

export const sortProviders = (providers: {
  [index: string]: ProviderRepresentation;
}) => {
  return [...new Map(Object.entries(providers).sort(sortProvider)).keys()];
};

const sortProvider = (
  a: [string, ProviderRepresentation],
  b: [string, ProviderRepresentation]
) => {
  let s1, s2;
  if (a[1].order !== b[1].order) {
    s1 = b[1].order;
    s2 = a[1].order;
  } else {
    s1 = a[0];
    s2 = b[0];
  }
  if (s1 < s2) {
    return -1;
  } else if (s1 > s2) {
    return 1;
  } else {
    return 0;
  }
};

export const exportClient = (client: ClientRepresentation): void => {
  const clientCopy = _.cloneDeep(client);
  delete clientCopy.id;

  if (clientCopy.protocolMappers) {
    for (let i = 0; i < clientCopy.protocolMappers.length; i++) {
      delete clientCopy.protocolMappers[i].id;
    }
  }

  FileSaver.saveAs(
    new Blob([JSON.stringify(clientCopy, null, 2)], {
      type: "application/json",
    }),
    clientCopy.clientId + ".json"
  );
};

export const toUpperCase = <T extends string>(name: T) =>
  (name.charAt(0).toUpperCase() + name.slice(1)) as Capitalize<T>;

export const convertToFormValues = (
  obj: any,
  prefix: string,
  setValue: (name: string, value: any) => void
) => {
  return Object.keys(obj).map((key) => {
    const newKey = key.replace(/\./g, "-");
    setValue(prefix + "." + newKey, obj[key]);
  });
};

export const flatten = (
  obj: Record<string, any> | undefined,
  path = ""
): {} => {
  if (!(obj instanceof Object)) return { [path.replace(/\.$/g, "")]: obj };

  return Object.keys(obj).reduce((output, key) => {
    return obj instanceof Array
      ? {
          ...output,
          ...flatten(obj[key as unknown as number], path + "[" + key + "]."),
        }
      : { ...output, ...flatten(obj[key], path + key + ".") };
  }, {});
};

export const convertFormValuesToObject = (
  obj: any,
  firstInstanceOnly?: boolean
) => {
  const keyValues = Object.keys(obj).map((key) => {
    const newKey = firstInstanceOnly
      ? key.replace(/-/, ".")
      : key.replace(/-/g, ".");
    return { [newKey]: obj[key] };
  });
  return Object.assign({}, ...keyValues);
};

export const emptyFormatter =
  (): IFormatter => (data?: IFormatterValueType) => {
    return data ? data : "—";
  };

export const upperCaseFormatter =
  (): IFormatter => (data?: IFormatterValueType) => {
    const value = data?.toString();

    return (value ? toUpperCase(value) : undefined) as string;
  };

export const getBaseUrl = (adminClient: KeycloakAdminClient) => {
  return (
    (adminClient.keycloak
      ? adminClient.keycloak.authServerUrl!
      : adminClient.baseUrl) + "/"
  );
};

export const emailRegexPattern =
  /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

export const forHumans = (seconds: number) => {
  const { t } = useTranslation();

  const levels: [
    [number, string],
    [number, string],
    [number, string],
    [number, string],
    [number, string]
  ] = [
    [Math.floor(seconds / 31536000), t("common:times.years")],
    [Math.floor((seconds % 31536000) / 86400), t("common:times.days")],
    [
      Math.floor(((seconds % 31536000) % 86400) / 3600),
      t("common:times.hours"),
    ],
    [
      Math.floor((((seconds % 31536000) % 86400) % 3600) / 60),
      t("common:times.minutes"),
    ],
    [(((seconds % 31536000) % 86400) % 3600) % 60, t("common:times.seconds")],
  ];
  let returntext = "";

  for (let i = 0, max = levels.length; i < max; i++) {
    if (levels[i][0] === 0) continue;
    returntext +=
      " " +
      levels[i][0] +
      " " +
      (levels[i][0] === 1
        ? levels[i][1].substr(0, levels[i][1].length - 1)
        : levels[i][1]);
  }
  return returntext.trim();
};

export const interpolateTimespan = (forHumans: string) => {
  const { t } = useTranslation();
  const timespan = forHumans.split(" ");

  if (timespan[1] === "Years") {
    return t(`realm-settings:convertedToYearsValue`, {
      convertedToYears: forHumans,
    });
  }

  if (timespan[1] === "Days") {
    return t(`realm-settings:convertedToDaysValue`, {
      convertedToYears: forHumans,
    });
  }

  if (timespan[1] === "Hours") {
    return t(`realm-settings:convertedToHoursValue`, {
      convertedToHours: forHumans,
    });
  }

  if (timespan[1] === "Minutes") {
    return t(`realm-settings:convertedToMinutesValue`, {
      convertedToMinutes: forHumans,
    });
  }

  if (timespan[1] === "Seconds") {
    return t(`realm-settings:convertedToSecondsValue`, {
      convertedToSeconds: forHumans,
    });
  }
};

export const KEY_PROVIDER_TYPE = "org.keycloak.keys.KeyProvider";
