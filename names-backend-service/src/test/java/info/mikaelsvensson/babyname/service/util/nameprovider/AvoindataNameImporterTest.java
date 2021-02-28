package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ResourceUtils;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AvoindataNameImporterTest {
    @Test
    void load_correctEncoding() throws IOException, NameException {
        // ARRANGE
        final var namesRepository = mock(NamesRepository.class);
        final var nameLouise = new Name("Louise", IdUtils.random());
        when(namesRepository.add(anyString(), nullable(User.class))).thenReturn(new Name("Test", IdUtils.random()));
        when(namesRepository.add(eq("Louise"), nullable(User.class))).thenReturn(nameLouise);

        final var importer = new AvoindataNameImporter(
                namesRepository,
                mock(UserRepository.class),
                false,
                new DefaultResourceLoader().getResource("classpath:names/fi/men-20210205.csv"),
                new DefaultResourceLoader().getResource("classpath:names/fi/women-20210205.csv"),
                mock(TaskScheduler.class)
        );

        // ACT
        importer.load();

        // ASSERT name with accented character is correctly read
        verify(namesRepository, times(1)).add(eq("H\u00E5kan"), nullable(User.class));
    }

}