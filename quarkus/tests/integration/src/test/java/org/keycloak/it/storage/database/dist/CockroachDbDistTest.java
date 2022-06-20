package org.keycloak.it.storage.database.dist;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.storage.database.CockroachDbTest;

@DistributionTest
@WithDatabase(alias = "cockroach")
public class CockroachDbDistTest extends CockroachDbTest {
}
