import FileSaver from "file-saver";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { ProviderRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

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
  const clientCopy = JSON.parse(JSON.stringify(client));
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

export const convertToFormValues = (
  obj: any,
  prefix: string,
  setValue: (name: string, value: any) => void
) => {
  return Object.keys(obj).map((key) => {
    const newKey = key.replace(/\./g, "_");
    setValue(prefix + "." + newKey, obj[key]);
  });
};

export const convertFormValuesToObject = (obj: any) => {
  const keyValues = Object.keys(obj).map((key) => {
    const newKey = key.replace(/_/g, ".");
    return { [newKey]: obj[key] };
  });
  return Object.assign({}, ...keyValues);
};
