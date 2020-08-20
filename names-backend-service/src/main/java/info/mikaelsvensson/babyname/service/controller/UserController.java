package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public User create() {
        return userRepository.add();
    }
}
