package info.mikaelsvensson.babyname.service.util.template;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTest {
    static class Context {
        final List<String> items;

        public Context(List<String> items) {
            this.items = items;
        }
    }

    @Test
    void renderTwoItemsInList() throws TemplateException {
        final var actual = new Template().render("Template.mustache", new Context(Arrays.asList("Alice", "Bob")));
        final var expected = "- Alice\n- Bob\n";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void renderEmptyList() throws TemplateException {
        final var actual = new Template().render("Template.mustache", new Context(Collections.emptyList()));
        final var expected = "No items\n";
        assertThat(actual).isEqualTo(expected);
    }
}