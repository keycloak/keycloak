package org.keycloak.testutils;

import org.keycloak.models.LDAPConstants;
import org.picketbox.test.ldap.AbstractLDAPTest;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * Forked from Picketlink project
 *
 * Abstract base for all LDAP test suites. It handles
 * @author Peter Skopek: pskopek at redhat dot com
 *
 */
public class LDAPEmbeddedServer extends AbstractLDAPTest {

    public static final String CONNECTION_PROPERTIES = "ldap/ldap-connection.properties";

    protected String connectionUrl = "ldap://localhost:10389";
    protected String baseDn =  "dc=keycloak,dc=org";
    protected String userDnSuffix = "ou=People,dc=keycloak,dc=org";
    protected String rolesDnSuffix = "ou=Roles,dc=keycloak,dc=org";
    protected String groupDnSuffix = "ou=Groups,dc=keycloak,dc=org";
    protected String agentDnSuffix = "ou=Agent,dc=keycloak,dc=org";
    protected boolean startEmbeddedLdapLerver = true;
    protected String bindDn = "uid=admin,ou=system";
    protected String bindCredential = "secret";
    protected String vendor = LDAPConstants.VENDOR_OTHER;
    protected boolean connectionPooling = true;
    protected boolean pagination = true;
    protected int batchSizeForSync = LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC;
    protected String usernameLDAPAttribute;
    protected String userObjectClasses;
    protected boolean userAccountControlsAfterPasswordUpdate;

    public static String IDM_TEST_LDAP_CONNECTION_URL = "idm.test.ldap.connection.url";
    public static String IDM_TEST_LDAP_BASE_DN = "idm.test.ldap.base.dn";
    public static String IDM_TEST_LDAP_ROLES_DN_SUFFIX = "idm.test.ldap.roles.dn.suffix";
    public static String IDM_TEST_LDAP_GROUP_DN_SUFFIX = "idm.test.ldap.group.dn.suffix";
    public static String IDM_TEST_LDAP_USER_DN_SUFFIX = "idm.test.ldap.user.dn.suffix";
    public static String IDM_TEST_LDAP_AGENT_DN_SUFFIX = "idm.test.ldap.agent.dn.suffix";
    public static String IDM_TEST_LDAP_START_EMBEDDED_LDAP_SERVER = "idm.test.ldap.start.embedded.ldap.server";
    public static String IDM_TEST_LDAP_BIND_DN = "idm.test.ldap.bind.dn";
    public static String IDM_TEST_LDAP_BIND_CREDENTIAL = "idm.test.ldap.bind.credential";
    public static String IDM_TEST_LDAP_VENDOR = "idm.test.ldap.vendor";
    public static String IDM_TEST_LDAP_CONNECTION_POOLING = "idm.test.ldap.connection.pooling";
    public static String IDM_TEST_LDAP_PAGINATION = "idm.test.ldap.pagination";
    public static String IDM_TEST_LDAP_BATCH_SIZE_FOR_SYNC = "idm.test.ldap.batch.size.for.sync";
    public static String IDM_TEST_LDAP_USERNAME_LDAP_ATTRIBUTE = "idm.test.ldap.username.ldap.attribute";
    public static String IDM_TEST_LDAP_USER_OBJECT_CLASSES = "idm.test.ldap.user.object.classes";
    public static String IDM_TEST_LDAP_USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE = "idm.test.ldap.user.account.controls.after.password.update";


    public LDAPEmbeddedServer() {
        super();
        loadConnectionProperties();
    }

    protected void loadConnectionProperties() {
        Properties p = new Properties();
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONNECTION_PROPERTIES);
            p.load(is);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        connectionUrl = p.getProperty(IDM_TEST_LDAP_CONNECTION_URL, connectionUrl);
        baseDn = p.getProperty(IDM_TEST_LDAP_BASE_DN, baseDn);
        userDnSuffix = p.getProperty(IDM_TEST_LDAP_USER_DN_SUFFIX, userDnSuffix);
        rolesDnSuffix = p.getProperty(IDM_TEST_LDAP_ROLES_DN_SUFFIX, rolesDnSuffix);
        groupDnSuffix = p.getProperty(IDM_TEST_LDAP_GROUP_DN_SUFFIX, groupDnSuffix);
        agentDnSuffix = p.getProperty(IDM_TEST_LDAP_AGENT_DN_SUFFIX, agentDnSuffix);
        startEmbeddedLdapLerver = Boolean.parseBoolean(p.getProperty(IDM_TEST_LDAP_START_EMBEDDED_LDAP_SERVER, "true"));
        bindDn = p.getProperty(IDM_TEST_LDAP_BIND_DN, bindDn);
        bindCredential = p.getProperty(IDM_TEST_LDAP_BIND_CREDENTIAL, bindCredential);
        vendor = p.getProperty(IDM_TEST_LDAP_VENDOR);
        connectionPooling = Boolean.parseBoolean(p.getProperty(IDM_TEST_LDAP_CONNECTION_POOLING, "true"));
        pagination = Boolean.parseBoolean(p.getProperty(IDM_TEST_LDAP_PAGINATION, "true"));
        batchSizeForSync = Integer.parseInt(p.getProperty(IDM_TEST_LDAP_BATCH_SIZE_FOR_SYNC, String.valueOf(batchSizeForSync)));
        usernameLDAPAttribute = p.getProperty(IDM_TEST_LDAP_USERNAME_LDAP_ATTRIBUTE);
        userObjectClasses = p.getProperty(IDM_TEST_LDAP_USER_OBJECT_CLASSES);
        userAccountControlsAfterPasswordUpdate = Boolean.parseBoolean(p.getProperty(IDM_TEST_LDAP_USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE));
    }

    @Override
    public void setup() throws Exception {
        // suppress emb. LDAP server start
        if (isStartEmbeddedLdapLerver()) {
            // On Windows, the directory may not be fully deleted from previous test
            String tempDir = System.getProperty("java.io.tmpdir");
            File workDir = new File(tempDir + File.separator + "server-work");
            if (workDir.exists()) {
                recursiveDeleteDir(workDir);
            }

            super.setup();
        }
    }

    @Override
    public void tearDown() throws Exception {
        // suppress emb. LDAP server stop
        if (isStartEmbeddedLdapLerver()) {

            // clear data left in LDAP
            DirContext ctx = getDirContext();
            clearSubContexts(ctx, new CompositeName(baseDn));

            super.tearDown();
        }
    }

    private DirContext getDirContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, connectionUrl);
        env.put(Context.SECURITY_PRINCIPAL, bindDn);
        env.put(Context.SECURITY_CREDENTIALS, bindCredential);
        DirContext ctx = new InitialDirContext(env);
        return ctx;
    }

    public Map<String,String> getLDAPConfig() {
        Map<String,String> ldapConfig = new HashMap<String,String>();
        ldapConfig.put(LDAPConstants.CONNECTION_URL, getConnectionUrl());
        ldapConfig.put(LDAPConstants.BASE_DN, getBaseDn());
        ldapConfig.put(LDAPConstants.BIND_DN, getBindDn());
        ldapConfig.put(LDAPConstants.BIND_CREDENTIAL, getBindCredential());
        ldapConfig.put(LDAPConstants.USER_DN_SUFFIX, getUserDnSuffix());
        ldapConfig.put(LDAPConstants.VENDOR, getVendor());
        ldapConfig.put(LDAPConstants.CONNECTION_POOLING, String.valueOf(isConnectionPooling()));
        ldapConfig.put(LDAPConstants.PAGINATION, String.valueOf(isPagination()));
        ldapConfig.put(LDAPConstants.BATCH_SIZE_FOR_SYNC, String.valueOf(getBatchSizeForSync()));
        ldapConfig.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, getUsernameLDAPAttribute());
        ldapConfig.put(LDAPConstants.USER_OBJECT_CLASSES, getUserObjectClasses());
        ldapConfig.put(LDAPConstants.USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE, String.valueOf(isUserAccountControlsAfterPasswordUpdate()));
        return ldapConfig;
    }


    public static void clearSubContexts(DirContext ctx, Name name) throws NamingException {

        NamingEnumeration<NameClassPair> enumeration = null;
        try {
            enumeration = ctx.list(name);
            while (enumeration.hasMore()) {
                NameClassPair pair = enumeration.next();
                Name childName = ctx.composeName(new CompositeName(pair.getName()), name);
                try {
                    ctx.destroySubcontext(childName);
                }
                catch (ContextNotEmptyException e) {
                    clearSubContexts(ctx, childName);
                    ctx.destroySubcontext(childName);
                }
            }
        }
        catch (NamingException e) {
            e.printStackTrace();
        }
        finally {
            try {
                enumeration.close();
            }
            catch (Exception e) {
                // Never mind this
            }
        }
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public String getUserDnSuffix() {
        return userDnSuffix;
    }

    public String getRolesDnSuffix() {
        return rolesDnSuffix;
    }

    public String getGroupDnSuffix() {
        return groupDnSuffix;
    }

    public String getAgentDnSuffix() {
        return agentDnSuffix;
    }

    public boolean isStartEmbeddedLdapLerver() {
        return startEmbeddedLdapLerver;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindCredential() {
        return bindCredential;
    }

    public String getVendor() {
        return vendor;
    }

    public boolean isConnectionPooling() {
        return connectionPooling;
    }

    public boolean isPagination() {
        return pagination;
    }

    public int getBatchSizeForSync() {
        return batchSizeForSync;
    }

    public String getUsernameLDAPAttribute() {
        return usernameLDAPAttribute;
    }

    public String getUserObjectClasses() {
        return userObjectClasses;
    }

    public boolean isUserAccountControlsAfterPasswordUpdate() {
        return userAccountControlsAfterPasswordUpdate;
    }

    @Override
    public void importLDIF(String fileName) throws Exception {
        // import LDIF only in case we are running against embedded LDAP server
        if (isStartEmbeddedLdapLerver()) {
            super.importLDIF(fileName);
        }
    }

    @Override
    protected void createBaseDN() throws Exception {
        ds.createBaseDN("keycloak", "dc=keycloak,dc=org");
    }

}
