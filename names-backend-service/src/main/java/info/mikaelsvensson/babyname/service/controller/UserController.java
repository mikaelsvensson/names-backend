package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

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
}
