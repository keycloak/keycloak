
import { KEY_PROVIDER_TYPE } from "../../util";
import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
// TIDECLOAK IMPLEMENTATION

export const findTideComponent = async (adminClient: KeycloakAdminClient, realm: string) => {
    const components = await adminClient.components.find({
        realm: realm,
        type: KEY_PROVIDER_TYPE,
    });
    return components.find((c) => c.providerId === "tide-vendor-key");
};

export const createTideComponent = async (adminClient: KeycloakAdminClient, realm: string, serverInfo: ServerInfoRepresentation) => {
    const allComponentTypes = serverInfo.componentTypes?.[KEY_PROVIDER_TYPE];
    const tideProps = allComponentTypes?.find((type) => type.id === "tide-vendor-key")?.properties;

    const keyValueProps = (tideProps ?? []).reduce((acc, prop) => {
        if (prop.defaultValue && typeof prop.defaultValue === "string") {
            acc[prop.name!] = [prop.defaultValue]; // Wrap the string in an array
        } else {
            acc[prop.name!] = []; // Assign an empty array if defaultValue is undefined or not a string
        }
        return acc;
    }, {} as { [key: string]: string[] }); // Ensuring the value is always string[]

    const newComponent = {
        name: "tide-vendor-key",
        config: Object.fromEntries(
            Object.entries(keyValueProps).map(([key, value]) => [
                key,
                Array.isArray(value) ? value : [value],
            ])
        ),
    };

    await adminClient.components.create({
        ...newComponent,
        providerId: "tide-vendor-key",
        providerType: KEY_PROVIDER_TYPE,
    });

    return await findTideComponent(adminClient, realm);
};