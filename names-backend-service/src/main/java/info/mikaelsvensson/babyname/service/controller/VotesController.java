package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping("votes")
public class VotesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(VotesController.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VotesRepository votesRepository;

    @Autowired
    private NamesRepository namesRepository;

    @GetMapping()
    public Collection<Vote> getVotes(Authentication authentication) {
        var userId = authentication.getName();
        try {
            return votesRepository.all(userRepository.get(userId));
        } catch (UserException | VoteException e) {
            LOGGER.warn("Could not read votes", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("{nameId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void setVotes(Authentication authentication, @PathVariable("nameId") String nameId, @RequestBody Vote vote) {
        var userId = authentication.getName();
        try {
            final var user = userRepository.get(userId);
            votesRepository.set(user, namesRepository.get(nameId, user), vote.getValue());
        } catch (UserException | NameException | VoteException e) {
            LOGGER.warn("Could not cast vote", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
