package org.keycloak.scim.resource.schema.path;

import java.util.List;

import org.keycloak.models.ModelValidationException;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.Email;
import org.keycloak.scim.resource.user.User;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selecting values of a complex multi-valued attribute with a filter on a sub-attribute other than {@code value}
 * (RFC 7644 section 3.5.2), as Microsoft Entra ID does when a user's email changes:
 * {@code replace emails[type eq "work"].value}.
 */
public class PathTest {

    private static final Attribute<UserModel, User> EMAILS = Attribute.<UserModel, User>complex("emails", Email.class)
            .multivalued()
            .withModelSetter((UserModel model, String value) -> { })
            .build()
            .get(0);

    private static Path path(String raw) {
        // the schema is only read for a missing path
        return new Path(null, raw);
    }

    private static JsonNode current(Email... emails) {
        return JsonSerialization.mapper.valueToTree(List.of(emails));
    }

    @Test
    public void entraWorkEmailFilterSelectsTheCurrentEmail() {
        Path path = path("emails[type eq \"work\"].value");

        assertFalse(path.filtersOnlyOnValue());
        assertEquals("emails.value", path.getPath());

        ArrayNode matched = path.select(EMAILS, current(new Email("jane.smith@northwind.example")));

        assertEquals(1, matched.size());
        assertEquals("jane.smith@northwind.example", matched.get(0).get("value").asText());
    }

    @Test
    public void typeComparisonIgnoresCase() {
        assertEquals(1, path("emails[type eq \"Work\"]").select(EMAILS, current(new Email("a@x.example"))).size());
    }

    @Test
    public void selectsOnlyTheEntriesTheFilterMatches() {
        JsonNode both = current(new Email("work@x.example", "work", true), new Email("home@x.example", "home", false));

        ArrayNode home = path("emails[type eq \"home\"]").select(EMAILS, both);
        assertEquals(1, home.size());
        assertEquals("home@x.example", home.get(0).get("value").asText());

        assertEquals(1, path("emails[primary eq true]").select(EMAILS, both).size());
        assertEquals(2, path("emails[type eq \"home\" or type eq \"work\"]").select(EMAILS, both).size());
        assertEquals(1, path("emails[type eq \"work\" and primary eq true]").select(EMAILS, both).size());
        assertEquals(1, path("emails[not (type eq \"work\")]").select(EMAILS, both).size());
    }

    @Test
    public void matchesNothingWhenNoEntryFitsOrThereAreNone() {
        assertEquals(0, path("emails[type eq \"home\"]").select(EMAILS, current(new Email("a@x.example"))).size());
        assertEquals(0, path("emails[type eq \"work\"]").select(EMAILS, null).size());
    }

    @Test
    public void refusesASubAttributeTheTypeDoesNotHave() {
        ModelValidationException error = assertThrows(ModelValidationException.class,
                () -> path("emails[kind eq \"work\"].value").select(EMAILS, current(new Email("a@x.example"))));
        assertTrue(error.getMessage().contains("Unknown attribute kind"), error.getMessage());
    }

    @Test
    public void subAttributeNamesIgnoreCase() {
        // RFC 7643 section 2.1: attribute names are case insensitive
        JsonNode both = current(new Email("work@x.example", "work", true), new Email("home@x.example", "home", false));

        assertEquals("work@x.example", path("emails[TYPE eq \"work\"].value").select(EMAILS, both).get(0).get("value").asText());
        assertEquals("work@x.example", path("emails[Primary eq true]").select(EMAILS, both).get(0).get("value").asText());
        assertEquals(2, path("emails[Value pr]").select(EMAILS, both).size());
    }

    @Test
    public void refusesAnOperatorThatDoesNotApplyToTheSubAttribute() {
        JsonNode work = current(new Email("work@x.example", "work", true));

        // a boolean only takes eq and ne, so "tru" is not a prefix match of true
        for (String operator : List.of("co", "sw", "ew", "gt")) {
            ModelValidationException error = assertThrows(ModelValidationException.class,
                    () -> path("emails[primary " + operator + " \"tru\"]").select(EMAILS, work));
            assertTrue(error.getMessage().contains("'" + operator + "' is not supported for primary"), error.getMessage());
        }

        // a string takes no ordering operator
        assertThrows(ModelValidationException.class, () -> path("emails[type gt \"work\"]").select(EMAILS, work));
    }

    @Test
    public void checksTheWholeFilterBeforeMatching() {
        JsonNode work = current(new Email("work@x.example", "work", true));

        // the first branch matches, so evaluation alone would never reach the unsupported second one
        assertThrows(ModelValidationException.class,
                () -> path("emails[type eq \"work\" or type gt \"work\"]").select(EMAILS, work));
        assertThrows(ModelValidationException.class,
                () -> path("emails[type eq \"home\" and primary co \"tru\"]").select(EMAILS, work));
        assertThrows(ModelValidationException.class,
                () -> path("emails[type eq \"work\" or kind eq \"work\"]").select(EMAILS, work));

        // with no entries at all, an invalid filter is still refused rather than matching nothing
        assertThrows(ModelValidationException.class, () -> path("emails[type gt \"work\"]").select(EMAILS, null));
    }

    @Test
    public void aValueOnlyFilterKeepsTheValueRule() {
        assertTrue(path("members[value eq \"123\"]").filtersOnlyOnValue());
        assertTrue(path("members[value eq \"1\" or value eq \"2\"]").filtersOnlyOnValue());
        assertFalse(path("members[display eq \"jane\"]").filtersOnlyOnValue());
        assertTrue(path("emails").filtersOnlyOnValue());
    }
}
