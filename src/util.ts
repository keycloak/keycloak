import { IFormatter, IFormatterValueType } from "@patternfly/react-table";
import FileSaver from "file-saver";
import _ from "lodash";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { ProviderRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import KeycloakAdminClient from "keycloak-admin";

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

export const toUpperCase = (name: string) =>
  name.charAt(0).toUpperCase() + name.slice(1);

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

export const convertFormValuesToObject = (obj: any) => {
  const keyValues = Object.keys(obj).map((key) => {
    const newKey = key.replace(/-/g, ".");
    return { [newKey]: obj[key] };
  });
  return Object.assign({}, ...keyValues);
};

export const emptyFormatter = (): IFormatter => (
  data?: IFormatterValueType
) => {
  return data ? data : "—";
};

export const upperCaseFormatter = (): IFormatter => (
  data?: IFormatterValueType
) => {
  const value = data?.toString();

  return (value ? toUpperCase(value) : undefined) as string;
};

export const getBaseUrl = (adminClient: KeycloakAdminClient) => {
  return adminClient.keycloak
    ? adminClient.keycloak.authServerUrl!
    : adminClient.baseUrl + "/";
};

export const emailRegexPattern = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
