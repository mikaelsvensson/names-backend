package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.NameBase;
import info.mikaelsvensson.babyname.service.repository.NamesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("names")
public class NamesController {

    public static enum CountRange {
        VERY_POPULAR(50_000, Integer.MAX_VALUE),
        POPULAR(10_000, 50_000),
        NOT_COMMON(1_000, 10_000),
        UNUSUAL(100, 1_000),
        VERY_UNUSUAL(0, 100);

        private final int min;
        private final int max;

        CountRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        boolean inRange(Name name) {
            return name.getCount() != null && name.getCount() >= min && name.getCount() < max;
        }
    }

    public static enum SexFilter {
        ANY,
        ONLY_MALE,
        ONLY_FEMALE,
        UNISEX
    }

    @Autowired
    private NamesRepository namesRepository;

    public NamesController() throws IOException {
    }

    @GetMapping
    public SearchResult get(
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit,
            @RequestParam(name = "popularity", required = false) CountRange countRange
    ) {
        return new SearchResult(namesRepository.all().stream()
                .filter(name -> namePrefix == null || namePrefix.trim().length() == 0 || name.getName().toLowerCase().startsWith(namePrefix.toLowerCase()))
                .filter(name -> countRange == null || countRange.inRange(name))
                .limit(Math.max(0, Math.min(limit, 1000)))
                .collect(Collectors.toList()));
    }

    public static class SearchResult {
        public List<Name> names;

        public SearchResult(List<Name> names) {
            this.names = names;
        }
    }
}
