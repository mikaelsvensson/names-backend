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

    @Autowired
    private NamesRepository namesRepository;

    public NamesController() throws IOException {
    }

    @GetMapping
    public SearchResult get(@RequestParam(name = "filter", required = false) String filter) {
        return new SearchResult(namesRepository.all().stream()
                .map(NameBase::getName)
                .filter(name -> filter == null || filter.trim().length() == 0 || name.toLowerCase().startsWith(filter.toLowerCase()))
                .collect(Collectors.toList()));
    }

    @PostMapping
    public Name create(@RequestBody NameBase nameBase) {
        return namesRepository.add(nameBase.getName(), nameBase.isMale(), nameBase.isFemale());
    }

    public static class SearchResult {
        public List<String> names;

        public SearchResult(List<String> names) {
            this.names = names;
        }
    }
}
