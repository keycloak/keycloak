import type { KeycloakAdminClient } from "../src/client.js";
import type OrganizationRepresentation from "../src/defs/organizationRepresentation.js";

declare const organizations: KeycloakAdminClient["organizations"];

const nullableResult = organizations.findOne(
  { id: "organization-id" },
  { catchNotFound: true },
);
const acceptsNullable: Promise<OrganizationRepresentation | null> =
  nullableResult;

const strictResult = organizations.findOne({ id: "organization-id" });
const acceptsStrict: Promise<OrganizationRepresentation> = strictResult;

type FindOneReturnType = Awaited<
  ReturnType<KeycloakAdminClient["organizations"]["findOne"]>
>;
const acceptsNullFromReturnType: FindOneReturnType = null;

void acceptsNullable;
void acceptsStrict;
void acceptsNullFromReturnType;
