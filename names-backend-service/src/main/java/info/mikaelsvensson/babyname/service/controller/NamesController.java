package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.util.ScbNameImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("names")
public class NamesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamesController.class);

    @Autowired
    private NamesRepository namesRepository;

    public NamesController() throws IOException {
    }

    @Autowired
    private ScbNameImporter scbNameImporter;

    @GetMapping
    public SearchResult get(
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "result-offset", required = false, defaultValue = "0") int offset,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit
    ) {
        try {
            final var names = namesRepository.all(Set.of(scbNameImporter.getUser().getId()), namePrefix, offset, limit + 1, null, null);
            final var isLast = names.size() < limit + 1;
            return new SearchResult(isLast ? names : names.subList(0, limit), isLast);
        } catch (NameException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{nameId}")
    public Name get(
            @PathVariable(name = "nameId", required = false) String nameId
    ) {
        try {
            return namesRepository.get(nameId);
        } catch (NameException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static class SearchResult {
        public final List<Name> names;
        public final boolean isLast;

        public SearchResult(List<Name> names, boolean isLast) {
            this.names = names;
            this.isLast = isLast;
        }
    }
}
