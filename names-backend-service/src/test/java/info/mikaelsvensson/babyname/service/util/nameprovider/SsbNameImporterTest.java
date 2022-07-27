package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SsbNameImporterTest {
    @Test
    void load_correctEncoding() throws IOException, NameException {
        // ARRANGE
        final var namesRepository = mock(NamesRepository.class);
        final var nameLouise = new Name("Louise", IdUtils.random());
        when(namesRepository.add(anyString(), nullable(User.class))).thenReturn(new Name("Test", IdUtils.random()));
        when(namesRepository.add(eq("Louise"), nullable(User.class))).thenReturn(nameLouise);

        final var importer = new SsbNameImporter(
                namesRepository,
                mock(UserRepository.class),
                new DefaultResourceLoader().getResource("classpath:names/no/boys.csv"),
                new DefaultResourceLoader().getResource("classpath:names/no/girls.csv")
        );

        // ACT
        importer.load();

        // ASSERT name with accented character is correctly read
        verify(namesRepository, times(1)).add(eq("H\u00E5kon"), nullable(User.class));
    }

}