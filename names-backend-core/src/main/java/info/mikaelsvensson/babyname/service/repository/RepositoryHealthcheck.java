package info.mikaelsvensson.babyname.service.repository;

import org.springframework.stereotype.Service;

@Service
public interface RepositoryHealthcheck {
    void ping() throws Exception;
}
