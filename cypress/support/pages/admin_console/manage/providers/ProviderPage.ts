export default class ProviderPage {
  // KerberosSettingsRequired required input values
  private kerberosNameInput = "data-testid=kerberos-name";
  private kerberosRealmInput = "data-testid=kerberos-realm";
  private kerberosPrincipalInput = "data-testid=kerberos-principal";
  private kerberosKeytabInput = "data-testid=kerberos-keytab";

  // LdapSettingsGeneral required input values
  private ldapNameInput = "data-testid=ldap-name";
  private ldapVendorInput = "#kc-vendor";
  private ldapVendorList = "#kc-vendor + ul";

  // LdapSettingsConnection required input values
  private ldapConnectionUrlInput = "data-testid=ldap-connection-url";
  private ldapBindTypeInput = "#kc-bind-type";
  private ldapBindTypeList = "#kc-bind-type + ul";
  private ldapBindDnInput = "data-testid=ldap-bind-dn";
  private ldapBindCredsInput = "data-testid=ldap-bind-credentials";

  // LdapSettingsSearching required input values
  private ldapUsersDnInput = "data-testid=ldap-users-dn";
  private ldapUserLdapAttInput = "data-testid=ldap-username-attribute";
  private ldapRdnLdapAttInput = "data-testid=ldap-rdn-attribute";
  private ldapUuidLdapAttInput = "data-testid=ldap-uuid-attribute";
  private ldapUserObjClassesInput = "data-testid=ldap-user-object-classes";

  // SettingsCache input values
  private cacheDayInput = "#kc-eviction-day";
  private cacheDayList = "#kc-eviction-day + ul";
  private cacheHourInput = "#kc-eviction-hour";
  private cacheHourList = "#kc-eviction-hour + ul";
  private cacheMinuteInput = "#kc-eviction-minute";
  private cacheMinuteList = "#kc-eviction-minute + ul";
  private cachePolicyInput = "#kc-cache-policy";
  private cachePolicyList = "#kc-cache-policy + ul";

  // Mapper required input values
  private userModelAttInput = "data-testid=mapper-userModelAttribute-fld";
  private ldapAttInput = "data-testid=mapper-ldapAttribute-fld";
  private userModelAttNameInput =
    "data-testid=mapper-userModelAttributeName-fld";
  private attValueInput = "data-testid=mapper-attributeValue-fld";
  private ldapFullNameAttInput = "data-testid=mapper-fullNameAttribute-fld";
  private ldapAttNameInput = "data-testid=mapper-ldapAttributeName-fld";
  private ldapAttValueInput = "data-testid=mapper-ldapAttributeValue-fld";
  private groupInput = "data-testid=mapper-group-fld";
  private ldapDnInput = "data-testid=ldap-dn";

  // mapper types
  private msadUserAcctMapper = "msad-user-account-control-mapper";
  private msadLdsUserAcctMapper = "msad-lds-user-account-control-mapper";
  private userAttLdapMapper = "user-attribute-ldap-mapper";
  private hcAttMapper = "hardcoded-attribute-mapper";
  private certLdapMapper = "certificate-ldap-mapper";
  private fullNameLdapMapper = "full-name-ldap-mapper";
  private hcLdapAttMapper = "hardcoded-ldap-attribute-mapper";
  private hcLdapGroupMapper = "hardcoded-ldap-group-mapper";
  private groupLdapMapper = "group-ldap-mapper";
  private roleLdapMapper = "role-ldap-mapper";
  private hcLdapRoleMapper = "hardcoded-ldap-role-mapper";

  private tab = "#pf-tab-serviceAccount-serviceAccount";
  private scopeTab = "scopeTab";
  private assignRole = "assignRole";
  private unAssign = "unAssignRole";
  private assign = "assign";
  private hide = "#hideInheritedRoles";
  private assignedRolesTable = "assigned-roles";
  private namesColumn = 'td[data-label="Name"]:visible';

  private rolesTab = "#pf-tab-roles-roles";
  private createRoleBtn = "data-testid=no-roles-for-this-client-empty-action";
  private realmRolesSaveBtn = "data-testid=realm-roles-save-button";
  private roleNameField = "#kc-name";
  private clientIdSelect = "#kc-client-id";

  private groupName = "aa-uf-mappers-group";
  private clientName = "aa-uf-mappers-client";
  private roleName = "aa-uf-mappers-role";

  changeCacheTime(unit: string, time: string) {
    switch (unit) {
      case "day":
        cy.get(this.cacheDayInput).click();
        cy.get(this.cacheDayList).contains(time).click();
        break;
      case "hour":
        cy.get(this.cacheHourInput).click();
        cy.get(this.cacheHourList).contains(time).click();
        break;
      case "minute":
        cy.get(this.cacheMinuteInput).click();
        cy.get(this.cacheMinuteList).contains(time).click();
        break;
      default:
        console.log("Invalid cache time, must be 'day', 'hour', or 'minute'.");
        break;
    }
    return this;
  }

  deleteCardFromCard(card: string) {
    cy.get(`[data-testid=${card}-dropdown]`).click();
    cy.get('[data-testid="card-delete"]').click();
    return this;
  }

  deleteCardFromMenu(providerType: string, card: string) {
    this.clickExistingCard(card);
    cy.get('[data-testid="action-dropdown"]').click();
    cy.get(`[data-testid="delete-${providerType}-cmd"]`).click();
    return this;
  }

  fillKerberosRequiredData(
    name: string,
    realm: string,
    principal: string,
    keytab: string
  ) {
    if (name) {
      cy.get(`[${this.kerberosNameInput}]`).type(name);
    }
    if (realm) {
      cy.get(`[${this.kerberosRealmInput}]`).type(realm);
    }
    if (principal) {
      cy.get(`[${this.kerberosPrincipalInput}]`).type(principal);
    }
    if (keytab) {
      cy.get(`[${this.kerberosKeytabInput}]`).type(keytab);
    }
    return this;
  }

  fillLdapRequiredGeneralData(name: string, vendor: string) {
    if (name) {
      cy.get(`[${this.ldapNameInput}]`).clear().type(name);
    }
    if (vendor) {
      cy.get(this.ldapVendorInput).click();
      cy.get(this.ldapVendorList).contains(vendor).click();
    }
    return this;
  }

  fillLdapRequiredConnectionData(
    connectionUrl: string,
    bindType: string,
    bindDn?: string,
    bindCreds?: string
  ) {
    if (connectionUrl) {
      cy.get(`[${this.ldapConnectionUrlInput}]`).type(connectionUrl);
    }
    if (bindType) {
      cy.get(this.ldapBindTypeInput).click();
      cy.get(this.ldapBindTypeList).contains(bindType).click();
    }
    if (bindDn) {
      cy.get(`[${this.ldapBindDnInput}]`).type(bindDn);
    }
    if (bindCreds) {
      cy.get(`[${this.ldapBindCredsInput}]`).type(bindCreds);
    }
    return this;
  }

  fillLdapRequiredSearchingData(
    usersDn: string,
    userLdapAtt: string,
    rdnLdapAtt: string,
    uuidLdapAtt: string,
    userObjClasses: string
  ) {
    if (usersDn) {
      cy.get(`[${this.ldapUsersDnInput}]`).type(usersDn);
    }
    if (userLdapAtt) {
      cy.get(`[${this.ldapUserLdapAttInput}]`).type(userLdapAtt);
    }
    if (rdnLdapAtt) {
      cy.get(`[${this.ldapRdnLdapAttInput}]`).type(rdnLdapAtt);
    }
    if (uuidLdapAtt) {
      cy.get(`[${this.ldapUuidLdapAttInput}]`).type(uuidLdapAtt);
    }
    if (userObjClasses) {
      cy.get(`[${this.ldapUserObjClassesInput}]`).type(userObjClasses);
    }
    return this;
  }

  selectCacheType(cacheType: string) {
    cy.get(this.cachePolicyInput).click();
    cy.get(this.cachePolicyList).contains(cacheType).click();
    return this;
  }

  goToMappers() {
    cy.get(`[data-testid="ldap-mappers-tab"]`).click();
  }

  createRole(roleName: string) {
    cy.get(this.rolesTab).click();
    cy.wait(1000);
    cy.get(`[${this.createRoleBtn}]`).click();
    cy.wait(1000);
    cy.get(this.roleNameField).type(roleName);
    cy.wait(1000);
    cy.get(`[${this.realmRolesSaveBtn}]`).click();
    cy.wait(1000);
  }

  createNewMapper(mapperType: string) {
    const userModelAttValue = "firstName";
    const ldapAttValue = "cn";
    const ldapDnValue = "ou=groups";

    cy.contains("Add").click();
    cy.wait(1000);

    cy.get("#kc-providerId").click();
    cy.get("button").contains(mapperType).click();

    cy.get(`[data-testid="ldap-mapper-name"]`).type(`${mapperType}-test`);

    switch (mapperType) {
      case this.msadUserAcctMapper:
      case this.msadLdsUserAcctMapper:
        break;
      case this.userAttLdapMapper:
      case this.certLdapMapper:
        cy.get(`[${this.userModelAttInput}]`).type(userModelAttValue);
        cy.get(`[${this.ldapAttInput}]`).type(ldapAttValue);
        break;
      case this.hcAttMapper:
        cy.get(`[${this.userModelAttNameInput}]`).type(userModelAttValue);
        cy.get(`[${this.attValueInput}]`).type(ldapAttValue);
        break;
      case this.fullNameLdapMapper:
        cy.get(`[${this.ldapFullNameAttInput}]`).type(ldapAttValue);
        break;
      case this.hcLdapAttMapper:
        cy.get(`[${this.ldapAttNameInput}]`).type(userModelAttValue);
        cy.get(`[${this.ldapAttValueInput}]`).type(ldapAttValue);
        break;
      case this.hcLdapGroupMapper:
        cy.get(`[${this.groupInput}]`).type(this.groupName);
        break;
      case this.groupLdapMapper:
        cy.get(`[${this.ldapDnInput}]`).type(ldapDnValue);
        break;

      case this.roleLdapMapper:
        cy.get(`[${this.ldapDnInput}]`).type(ldapDnValue);
        // cy select clientID dropdown and choose clientName (var)
        cy.get(this.clientIdSelect).click();
        cy.get("button").contains(this.clientName).click({ force: true });
        break;

      case this.hcLdapRoleMapper:
        cy.get(`[data-testid="selectRole"]`).click();
        cy.wait(2000);
        cy.get(this.namesColumn)
          .contains(this.clientName)
          .parent()
          .parent()
          .within(() => {
            cy.get('input[name="radioGroup"]').click();
          });
        cy.getId(this.assign).click();
        break;
      default:
        console.log("Invalid mapper type.");
        break;
    }
  }

  updateMapper(mapperType: string) {
    const userModelAttValue = "lastName";
    const ldapAttValue = "sn";

    switch (mapperType) {
      case this.msadUserAcctMapper:
      case this.msadLdsUserAcctMapper:
        break;
      case this.userAttLdapMapper:
      case this.certLdapMapper:
        cy.get(`[${this.userModelAttInput}]`).clear();
        cy.get(`[${this.userModelAttInput}]`).type(userModelAttValue);
        cy.get(`[${this.ldapAttInput}]`).clear();
        cy.get(`[${this.ldapAttInput}]`).type(ldapAttValue);
        break;
      case this.hcAttMapper:
        cy.get(`[${this.userModelAttNameInput}]`).clear();
        cy.get(`[${this.userModelAttNameInput}]`).type(userModelAttValue);
        cy.get(`[${this.attValueInput}]`).clear();
        cy.get(`[${this.attValueInput}]`).type(ldapAttValue);
        break;
      case this.fullNameLdapMapper:
        cy.get(`[${this.ldapFullNameAttInput}]`).clear();
        cy.get(`[${this.ldapFullNameAttInput}]`).type(ldapAttValue);
        break;
      case this.hcLdapAttMapper:
        cy.get(`[${this.ldapAttNameInput}]`).clear();
        cy.get(`[${this.ldapAttNameInput}]`).type(userModelAttValue);
        cy.get(`[${this.ldapAttValueInput}]`).clear;
        cy.get(`[${this.ldapAttValueInput}]`).type(ldapAttValue);
        break;
      default:
        console.log("Invalid mapper name.");
        break;
    }
  }

  clickExistingCard(cardName: string) {
    cy.get('[data-testid="keycloak-card-title"]').contains(cardName).click();
    cy.wait(1000);
    return this;
  }

  clickMenuCommand(menu: string, command: string) {
    cy.contains("button", menu).click();
    cy.contains("li", command).click();
    return this;
  }

  clickNewCard(providerType: string) {
    cy.get(`[data-testid=${providerType}-card]`).click();
    cy.wait(1000);
    return this;
  }

  disableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).uncheck({ force: true });
    return this;
  }

  enableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).check({ force: true });
    return this;
  }

  save(providerType: string) {
    cy.get(`[data-testid=${providerType}-save]`).click();
    return this;
  }

  cancel(providerType: string) {
    cy.get(`[data-testid=${providerType}-cancel]`).click();
    return this;
  }
}
