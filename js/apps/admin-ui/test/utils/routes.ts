import { generatePath } from "react-router-dom";
import type { Path, PathParam } from "react-router-dom";

type RouteParams<PathTemplate extends string> = {
  [Key in PathParam<PathTemplate>]: string;
};

function generateEncodedPath<PathTemplate extends string>(
  pathTemplate: PathTemplate,
  params: RouteParams<PathTemplate>,
): string {
  const encodedParams = { ...params };

  for (const key in encodedParams) {
    const pathKey = key as keyof RouteParams<PathTemplate>;
    encodedParams[pathKey] = encodeURIComponent(encodedParams[pathKey]) as
      | RouteParams<PathTemplate>[typeof pathKey]
      | string;
  }

  return generatePath(pathTemplate, encodedParams);
}

export function toAuthentication(params: {
  realm: string;
  tab?: "flows" | "required-actions" | "policies";
}): Partial<Path> {
  if (params.tab) {
    return {
      pathname: generateEncodedPath("/:realm/authentication/:tab", {
        realm: params.realm,
        tab: params.tab,
      }),
    };
  }

  return {
    pathname: generateEncodedPath("/:realm/authentication", {
      realm: params.realm,
    }),
  };
}

export function toClientScopes(params: { realm: string }): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/client-scopes", params),
  };
}

export function toNewClientScope(params: { realm: string }): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/client-scopes/new", params),
  };
}

export function toClients(params: {
  realm: string;
  tab?: "list" | "initial-access-token" | "client-registration";
}): Partial<Path> {
  if (params.tab) {
    return {
      pathname: generateEncodedPath("/:realm/clients/:tab", {
        realm: params.realm,
        tab: params.tab,
      }),
    };
  }

  return {
    pathname: generateEncodedPath("/:realm/clients", {
      realm: params.realm,
    }),
  };
}

export function toClient(params: {
  realm: string;
  clientId: string;
  tab:
    | "settings"
    | "keys"
    | "credentials"
    | "roles"
    | "clientScopes"
    | "advanced"
    | "mappers"
    | "authorization"
    | "serviceAccount"
    | "permissions"
    | "sessions"
    | "events"
    | "ssf";
}): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/clients/:clientId/:tab", params),
  };
}

export function toSsfClientTab(params: {
  realm: string;
  clientId: string;
  tab: "receiver" | "stream" | "subjects" | "event-search" | "emit-events";
}): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/clients/:clientId/ssf/:tab", params),
  };
}

export function toRealmSettings(params: {
  realm: string;
  tab?:
    | "general"
    | "login"
    | "email"
    | "themes"
    | "keys"
    | "events"
    | "localization"
    | "security-defenses"
    | "sessions"
    | "tokens"
    | "client-policies"
    | "user-profile"
    | "user-registration";
}): Partial<Path> {
  if (params.tab) {
    return {
      pathname: generateEncodedPath("/:realm/realm-settings/:tab", {
        realm: params.realm,
        tab: params.tab,
      }),
    };
  }

  return {
    pathname: generateEncodedPath("/:realm/realm-settings", {
      realm: params.realm,
    }),
  };
}

export function toAddUser(params: { realm: string }): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/users/add-user", params),
  };
}

export function toUser(params: {
  realm: string;
  id: string;
  tab:
    | "settings"
    | "groups"
    | "organizations"
    | "consents"
    | "attributes"
    | "sessions"
    | "credentials"
    | "role-mapping"
    | "identity-provider-links"
    | "events"
    | "workflows"
    | "verifiable-credentials";
}): Partial<Path> {
  return {
    pathname: generateEncodedPath("/:realm/users/:id/:tab", params),
  };
}

export function toUsers(params: {
  realm: string;
  tab?: "list" | "permissions";
}): Partial<Path> {
  if (params.tab) {
    return {
      pathname: generateEncodedPath("/:realm/users/:tab", {
        realm: params.realm,
        tab: params.tab,
      }),
    };
  }

  return {
    pathname: generateEncodedPath("/:realm/users", {
      realm: params.realm,
    }),
  };
}
