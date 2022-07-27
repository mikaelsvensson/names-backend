package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DstNameImporterTest {

    private static final double FILE_COUNT = 35.0;

    @Test
    void load_correctPercentAndEncoding() throws IOException, NameException {
        // ARRANGE
        final var namesRepository = mock(NamesRepository.class);
        final var nameLouise = new Name("Louise", IdUtils.random());
        when(namesRepository.add(anyString(), nullable(User.class))).thenReturn(new Name("Test", IdUtils.random()));
        when(namesRepository.add(eq("Louise"), nullable(User.class))).thenReturn(nameLouise);

        final var importer = new DstNameImporter(
                namesRepository,
                mock(UserRepository.class),
                ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader()).getResources("classpath:names/dk/*.html")
        );

        // ACT
        importer.load();

        // ASSERT percent calculation
        verify(namesRepository, times(1)).setDemographicsProperties(eq(nameLouise), eq(Country.DENMARK), eq(new PopulationProperties(
                (36 + 36 + 32 + 33 + 31 + 34 + 31 + 30 + 26 + 26 + 22 + 20 + 18 + 15 + 14 + 13 + 12 + 10 + 8 + 7 + 6 + 5) / 1000.0 / FILE_COUNT,
                1.0
        )));

        // ASSERT name with accented character is correctly read
        verify(namesRepository, times(1)).add(eq("L\u00E6rke"), nullable(User.class));
    }
}