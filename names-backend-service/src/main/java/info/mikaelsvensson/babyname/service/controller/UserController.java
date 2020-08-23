package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.NameBase;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VotesRepository votesRepository;

    @Autowired
    private NamesRepository namesRepository;

    @PostMapping
    public User create() {
        try {
            return userRepository.add();
        } catch (UserException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/votes")
    public Collection<Vote> getVotes(@PathVariable("userId") String userId) {
        try {
            return votesRepository.all(userRepository.get(userId));
        } catch (UserException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/votes/{nameId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void setVotes(@PathVariable("userId") String userId, @PathVariable("nameId") String nameId, @RequestBody Vote vote) {
        try {
            votesRepository.set(userRepository.get(userId), namesRepository.get(nameId), vote.getVoteType());
        } catch (UserException | NameException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/names")
    public NamesController.SearchResult get(
            @PathVariable("userId") String userId,
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit,
            @RequestParam(name = "popularity", required = false) NamesController.CountRange countRange
    ) {
        return new NamesController.SearchResult(namesRepository.all().stream()
                .filter(name -> name.getOwnerUserId() == null || userId.equals(name.getOwnerUserId()))
                .filter(name -> namePrefix == null || namePrefix.trim().length() == 0 || name.getName().toLowerCase().startsWith(namePrefix.toLowerCase()))
                .filter(name -> countRange == null || countRange.inRange(name))
                .limit(Math.max(0, Math.min(limit, 1000)))
                .collect(Collectors.toList()));
    }

    @PostMapping("/{userId}/names")
    @ResponseStatus(HttpStatus.CREATED)
    public Name create(@PathVariable("userId") String userId, @RequestBody NameBase nameBase) {
        try {
            return namesRepository.add(nameBase.getName(), nameBase.isMale(), nameBase.isFemale(), userRepository.get(userId).getId());
        } catch (UserException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
