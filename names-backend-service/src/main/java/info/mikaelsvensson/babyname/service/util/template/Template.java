package info.mikaelsvensson.babyname.service.util.template;

import com.github.mustachejava.DefaultMustacheFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;

@Component
public class Template {

    private final DefaultMustacheFactory mf = new DefaultMustacheFactory();

    public String render(String template, Object parameters) throws TemplateException {
        try {
            final var m = this.mf.compile(template);
            StringWriter writer = new StringWriter();
            m.execute(writer, parameters).flush();
            return writer.toString();
        } catch (IOException e) {
            throw new TemplateException("Could not render template " + template, e);
        }
    }
}
