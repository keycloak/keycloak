import OrganizationRepresentation from "js/libs/keycloak-admin-client/lib/defs/organizationRepresentation";
import UserRepresentation from "js/libs/keycloak-admin-client/lib/defs/userRepresentation";
import { capitalizeFirstLetterFormatter } from "../util";

type OrganizationMembershipTypeRepresentation = OrganizationRepresentation &
  UserRepresentation & {
    membershipType?: string;
  };

type UserOrganizationMembershipTypesProps = {
  memberships: OrganizationMembershipTypeRepresentation[];
  user: UserRepresentation;
};

export const getUserMembershipsWithTypes = ({
  memberships,
  user,
}: UserOrganizationMembershipTypesProps) => {
  const userMemberships = memberships.filter(
    (membership: OrganizationMembershipTypeRepresentation) =>
      membership.username === user.username,
  );

  const membershipType = userMemberships.map((membership: any) => {
    const formattedMembershipType = capitalizeFirstLetterFormatter()(
      membership.membershipType,
    );
    return formattedMembershipType;
  });

  return membershipType;
};
