package org.keycloak.representations.idm;

public class MembershipRepresentation extends OrganizationRepresentation {
    private MembershipType membershipType;

    public MembershipRepresentation() {
        super();
    }

    public MembershipRepresentation(OrganizationRepresentation rep) {
        super(rep);
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
    }
}
