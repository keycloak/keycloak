package org.keycloak.adapters.saml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.keycloak.adapters.saml.config.parsers.ResourceLoader;

public class DatabaseRoleMapper implements RoleMappingsProvider {

	private static final Logger log = Logger.getLogger(DatabaseRoleMapper.class.getName());

	public static final String PROVIDER_ID = "database-role-mapper";

	private static final String DS_JNDI_NAME = "dsJndiName";

	private static final String ROLES_QUERY = "rolesQuery";

	private String dsJndiName = "java:/DefaultDS";
	private String rolesQuery = "select distinct r.name from roletypeentity r, relationshipidentitytypeentity x1, relationshipidentitytypeentity x2, accounttypeentity a where r.id=x1.identitytype_id and x1.owner_id=x2.owner_id and x2.identitytype_id=a.id and a.loginname=?";

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void init(SamlDeployment deployment, ResourceLoader loader, Properties config) {
		String p = config.getProperty(DS_JNDI_NAME);
		if (p != null) {
			dsJndiName = p;
		}
		log.config(DS_JNDI_NAME + " = " + dsJndiName);
		p = config.getProperty(ROLES_QUERY);
		if (p != null) {
			rolesQuery = p;
		}
		log.config(ROLES_QUERY + " = " + rolesQuery);
	}

	@Override
	public Set<String> map(String principalName, Set<String> roles) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Set<String> res = new HashSet<String>(roles);
		try {
			InitialContext ctx = new InitialContext();
			DataSource ds = (DataSource) ctx.lookup(dsJndiName);
			conn = ds.getConnection();

			log.finest("Executing query " + rolesQuery + " with parameter " + principalName + " on " + dsJndiName);
			ps = conn.prepareStatement(rolesQuery);
			ps.setString(1, principalName);
			rs = ps.executeQuery();
			while (rs.next()) {
				String role = rs.getString(1);
				res.add(role);
				log.finest("Added role " + role);
			}
		} catch (NamingException ex) {
			log.warning("Failed to lookup datasource " + dsJndiName);
		} catch (SQLException ex) {
			log.warning("Failed to process query " + rolesQuery);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ex) {
				}
			}
		}
		return res;
	}
}
