package info.mikaelsvensson.babyname.service.util.auth;

import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorException;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnonymousAuthenticatorTest {

    @Test
    void getId_existingId() throws AnonymousAuthenticatorException, UserAuthenticatorException {
        // ARRANGE
        final var repository = mock(AnonymousAuthenticatorRepository.class);
        when(repository.exists("id")).thenReturn(true);

        final var authenticator = new AnonymousAuthenticator(repository, 1);

        // ACT
        final var actual = authenticator.getId("id");

        // ASSERT
        assertThat(actual).isEqualTo("id");
        verify(repository, times(1)).logUse("id");
    }

    @Test
    void getId_missingId() throws AnonymousAuthenticatorException, UserAuthenticatorException {
        // ARRANGE
        final var repository = mock(AnonymousAuthenticatorRepository.class);
        when(repository.exists("id")).thenReturn(false);

        final var authenticator = new AnonymousAuthenticator(repository, 1);

        assertThrows(UserAuthenticatorException.class, () -> {
            // ACT
            authenticator.getId("id");
        });

        // ASSERT
        verify(repository, times(0)).logUse("id");
    }

    @Test
    void createId_happyPath() throws AnonymousAuthenticatorException, UserAuthenticatorException {
        // ARRANGE
        final var repository = mock(AnonymousAuthenticatorRepository.class);
        when(repository.count()).thenReturn(1L);

        final var authenticator = new AnonymousAuthenticator(repository, 2);

        // ACT
        authenticator.createId();

        // ASSERT
        verify(repository, times(1)).count();
        verify(repository, times(1)).create();
    }

    @Test
    void createId_tooManyUsers() throws AnonymousAuthenticatorException {
        // ARRANGE
        final var repository = mock(AnonymousAuthenticatorRepository.class);
        when(repository.count()).thenReturn(2L);
        final var authenticator = new AnonymousAuthenticator(repository, 2);

        // ACT
        assertThrows(UserAuthenticatorException.class, authenticator::createId);

        // ASSERT
        verify(repository, times(1)).count();
        verify(repository, times(0)).create();
    }
}