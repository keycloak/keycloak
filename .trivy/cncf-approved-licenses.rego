package trivy

import data.lib.trivy

default ignore = false

# CNCF Approved Licenses
ignore {
    cncf_allowlist := { "Apache-2.0", "0BSD", "BSD-2-Clause", "BSD-3-Clause", "MIT", "MIT-0", "ISC" }
    input.Name == cncf_allowlist[_]
}

# CNCF Exceptions - https://github.com/cncf/foundation/issues/817
ignore {
    cncf_exceptions_817 := { "com.h2database:h2", "com.mysql:mysql-connector-j", "jakarta.annotation:jakarta.annotation-api", "jakarta.el:jakarta.el-api", "jakarta.interceptor:jakarta.interceptor-api", "jakarta.json:jakarta.json-api", "jakarta.resource:jakarta.resource-api", "jakarta.servlet:jakarta.servlet-api", "jakarta.transaction:jakarta.transaction-api", "jakarta.ws.rs:jakarta.ws.rs-api", "javax.xml.bind:jaxb-api", "org.eclipse.parsson:parsson", "org.graalvm.sdk:nativeimage", "org.graalvm.sdk:word", "org.hibernate.common:hibernate-commons-annotations", "org.hibernate.orm:hibernate-core", "org.hibernate.orm:hibernate-graalvm", "org.mariadb.jdbc:mariadb-java-client", "org.openjdk.nashorn:nashorn-core", "org.reactivestreams:reactive-streams" }
    input.PkgName == cncf_exceptions_817[_]
}

# CNCF Exceptions - https://github.com/cncf/foundation/issues/1177
ignore {
    input.PkgName == "org.glassfish.expressly:expressly"
}

# Multi licensed - Apache-2.0
ignore {
    apache_20 := { "org.jboss:jboss-transaction-spi", "org.hibernate.orm:hibernate-community-dialects", "net.java.dev.jna:jna", "net.java.dev.jna:jna-platform" }
    input.PkgName == apache_20[_]
}
ignore {
    startswith(input.PkgName, "io.vertx:vertx")
}

# Multi licensed - BSD-2-Clause
ignore {
    bsd2_clause := { "org.hdrhistogram:HdrHistogram", "org.latencyutils:LatencyUtils" }
    input.PkgName == bsd2_clause[_]
}

# Multi licensed - EDL 1.0 (BSD-3-Clause)
ignore {
    edl_10 := { "org.locationtech.jts:jts-core", "jakarta.mail:jakarta.mail-api", "jakarta.persistence:jakarta.persistence-api", "org.eclipse.angus:angus-mail" }
    input.PkgName == edl_10[_]
}

# Multi licensed - MIT
ignore {
    mit := { "jszip", "font-awesome", "pako" }
    input.PkgName == mit[_]
}