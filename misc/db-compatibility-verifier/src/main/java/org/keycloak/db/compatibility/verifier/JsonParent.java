package org.keycloak.db.compatibility.verifier;

import java.util.Collection;

record JsonParent(Collection<ChangeSet> changeSets, Collection<Migration> migrations) {}
