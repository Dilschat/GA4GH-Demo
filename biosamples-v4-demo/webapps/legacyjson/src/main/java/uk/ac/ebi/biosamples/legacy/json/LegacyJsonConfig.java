package uk.ac.ebi.biosamples.legacy.json;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import uk.ac.ebi.biosamples.legacy.json.domain.LegacyContactMixin;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyOrganizationMixin;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Organization;

@Configuration
public class LegacyJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
        b.indentOutput(true)
                .mixIn(Contact.class, LegacyContactMixin.class)
                .mixIn(Organization.class, LegacyOrganizationMixin.class);
        return b;
    }
}
