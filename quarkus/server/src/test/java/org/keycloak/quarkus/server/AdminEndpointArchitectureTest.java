/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.server;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;

import org.keycloak.services.resources.admin.AdminConsole;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmsAdminResourcePreflight;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * <h2>DISCLAIMER: this is a smoke detector, NOT a security control</h2>
 *
 * This test only verifies that each JAX-RS HTTP method of the admin resources <em>calls some</em> permission method
 * (a {@code void} or {@code boolean} method of the {@code ...admin.fgap} package, such as {@code requireManageRealm()}
 * or {@code canViewRealm()}), directly, via helpers in the admin resource classes it delegates to, or via a check in the constructor
 * or the creating sub-resource locator of its class. It scans all admin resources on the classpath of the Quarkus server. It catches the case of an endpoint that was
 * added without any permission check at all. Passing this test <strong>does not mean that an endpoint is protected</strong>.
 *
 * <h3>False negatives (insecure code that passes): realistic, and not rare</h3>
 * <ul>
 *     <li>The result of a {@code can*} method is ignored, or only used for something unrelated, like filtering a UI hint.</li>
 *     <li>The wrong permission is checked, for example a view permission on a delete endpoint, or the realm instead of the client.</li>
 *     <li>The check only happens on some code paths, for example inside an {@code if} or after the data was already changed.</li>
 *     <li>A check in a constructor or in the parent locator protects every method of the class, even when a method added later needs a stricter permission.</li>
 *     <li>A helper contains a check, but the endpoint doesn't reach it on all paths.</li>
 *     <li>The authorization is done elsewhere, or not at all, while the method calls an unrelated permission method.</li>
 *     <li>Sub-resource locators (methods with only {@code @Path}) are not checked, nor are the v2 admin API packages
 *     ({@link #UNSUPPORTED_PACKAGES}), nor endpoints whose JAX-RS annotations are declared on an interface.</li>
 *     <li>All classes in {@link #PUBLIC_ENDPOINT_CLASSES} are exempt, including everything added to that list in the future.</li>
 * </ul>
 *
 * <h3>False positives (secure code that fails): rare, but possible</h3>
 * <ul>
 *     <li>The check is performed in a class outside the {@code ..admin..} packages (a utility or a filter) that the analysis doesn't follow.</li>
 *     <li>The check lives in a lambda nested in another lambda, which the lookup by synthetic method name doesn't find.</li>
 *     <li>An endpoint is intentionally open to any authenticated user. It then needs an entry in the allowlist, with a reason.</li>
 * </ul>
 * At the time of writing, no existing endpoint was a false positive.
 *
 * <p>Authorization correctness must be verified by integration tests that call each admin endpoint as a user without the
 * required permissions and expect HTTP 403, and by code review. Do not remove those tests or reviews because this rule passes.
 */

@AnalyzeClasses(packages = "org.keycloak", importOptions = ImportOption.DoNotIncludeTests.class)
public class AdminEndpointArchitectureTest {
    private static final String PERMISSION_PACKAGE = "org.keycloak.services.resources.admin.fgap";

    // Not protected by admin permissions by design: the admin console (HTML, whoami and logout of the current user)
    // and the redirects and CORS preflight handling in the root resources.
    private static final Set<String> PUBLIC_ENDPOINT_CLASSES = Set.of(
            AdminConsole.class.getName(),
            AdminRoot.class.getName(),
            AdminCorsPreflightService.class.getName(),
            RealmsAdminResourcePreflight.class.getName());

    // The v2 admin API delegates authorization to the ClientResourceTypeProvider layer, which this rule does not follow,
    // so it is NOT covered by this test and needs to be reviewed separately.
    private static final Set<String> UNSUPPORTED_PACKAGES = Set.of(
            "org.keycloak.admin.api",
            "org.keycloak.rest.admin.api");

    @ArchTest
    public static final ArchRule ADMIN_ENDPOINTS_CHECK_PERMISSIONS = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("org.keycloak..admin..")
            .and().areDeclaredInClassesThat().resideOutsideOfPackage(PERMISSION_PACKAGE)
            .and(new DescribedPredicate<JavaMethod>("are JAX-RS HTTP endpoints of protected resources") {
                @Override
                public boolean test(JavaMethod method) {
                    // sub-resource locators only carry @Path; the methods of the returned resources are checked by this rule as well
                    return method.isMetaAnnotatedWith(HttpMethod.class)
                            && !method.getModifiers().contains(JavaModifier.ABSTRACT)
                            && !method.isAnnotatedWith(OPTIONS.class)
                            && !PUBLIC_ENDPOINT_CLASSES.contains(method.getOwner().getName())
                            && UNSUPPORTED_PACKAGES.stream().noneMatch(pkg -> method.getOwner().getPackageName().startsWith(pkg));
                }
            })
            .should(new ArchCondition<>("check admin permissions, directly or through helper methods of the same class") {
                @Override
                public void check(JavaMethod endpoint, ConditionEvents events) {
                    boolean valid = checksPermissions(endpoint, new HashSet<>()) || classProtected(endpoint.getOwner(), new HashSet<>());
                    events.add(new SimpleConditionEvent(endpoint, valid, endpoint.getFullName() + " does not check admin permissions"));
                }
            }).because("every admin endpoint must verify that the caller is allowed to perform the operation");

    // Methods like requireManageRealm() (void) or canViewRealm() (boolean) decide on access. Accessors like realm() or users()
    // only return another evaluator and don't check anything.
    private static boolean isPermissionDecision(JavaClass owner, JavaClass returnType) {
        return owner.getPackageName().equals(PERMISSION_PACKAGE)
                && (returnType.isEquivalentTo(void.class) || returnType.isEquivalentTo(boolean.class));
    }

    private static boolean isAdminResourceClass(JavaClass clazz) {
        return clazz.getPackageName().matches("org\\.keycloak\\..*\\badmin\\b.*") && !clazz.getPackageName().equals(PERMISSION_PACKAGE);
    }

    // A method or constructor checks permissions when it, a lambda in it, or an admin resource method it delegates to makes a decision
    private static boolean checksPermissions(JavaCodeUnit unit, Set<JavaCodeUnit> visited) {
        if (!visited.add(unit)) {
            return false;
        }
        for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
            if (isPermissionDecision(call.getTargetOwner(), call.getTarget().getRawReturnType())) {
                return true;
            }
        }
        for (var reference : unit.getMethodReferencesFromSelf()) {
            if (isPermissionDecision(reference.getTargetOwner(), reference.getTarget().getRawReturnType())) {
                return true;
            }
        }
        Set<JavaCodeUnit> next = new HashSet<>();
        unit.getMethodCallsFromSelf().forEach(call -> call.getTarget().resolveMember()
                .filter(target -> isAdminResourceClass(target.getOwner())).ifPresent(next::add));
        String lambdaPrefix = "lambda$" + (unit.isConstructor() ? "new" : unit.getName()) + "$";
        unit.getOwner().getAllMethods().stream().filter(method -> method.getName().startsWith(lambdaPrefix)).forEach(next::add);
        return next.stream().anyMatch(target -> checksPermissions(target, visited));
    }

    // A resource is protected as a whole when its constructor checks permissions, or when the sub-resource locator creating it does
    // (or the resource containing that locator is protected in turn).
    private static boolean classProtected(JavaClass clazz, Set<JavaClass> visited) {
        if (!visited.add(clazz)) {
            return false;
        }
        for (JavaConstructor constructor : clazz.getConstructors()) {
            if (checksPermissions(constructor, new HashSet<>())) {
                return true;
            }
        }
        for (JavaConstructorCall creation : clazz.getConstructorCallsToSelf()) {
            JavaCodeUnit creator = creation.getOrigin();
            boolean locator = creator instanceof JavaMethod method
                    && method.isAnnotatedWith(Path.class) && !method.isMetaAnnotatedWith(HttpMethod.class)
                    && !PUBLIC_ENDPOINT_CLASSES.contains(creator.getOwner().getName());
            if (locator && (checksPermissions(creator, new HashSet<>()) || classProtected(creator.getOwner(), visited))) {
                return true;
            }
        }
        return false;
    }
}
