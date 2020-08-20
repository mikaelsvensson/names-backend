package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InmemoryNamesRepository implements NamesRepository {

    @Value("classpath:names_sorted.txt")
    Resource database;

    private List<Name> names;

    private List<Name> getNames() {
        if (names == null) {
            try {
                names = Files.readAllLines(database.getFile().toPath()).stream()
                        .map(line -> Pattern.compile(",").split(line))
                        .filter(columns -> columns.length == 3)
                        .map(columns -> new Name(
                                columns[0],
                                !"".equals(columns[1]) ? Integer.parseInt(columns[1]) : null,
                                "m".equals(columns[2]),
                                "f".equals(columns[2])))
                        .collect(Collectors.toCollection(ArrayList::new));
            } catch (IOException e) {
                names = new ArrayList<>();
            }
        }
        return names;
    }

    @Override
    public List<Name> all() {
        return getNames();
    }

    @Override
    public Name add(String name, Boolean isMale, Boolean isFemale) {
        final var newObj = new Name(name, 0, isMale, isFemale);
        getNames().add(newObj);
        return newObj;
    }
}
