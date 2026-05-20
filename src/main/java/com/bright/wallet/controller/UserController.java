package com.bright.wallet.controller;

import com.bright.wallet.model.User;
import com.bright.wallet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public Object getUsers() {
        return userService.getUsers();
    }

    // @Valid activates all validation annotations on User
    // GlobalExceptionHandler now handles any errors — no try/catch needed here
    @PostMapping("/register")
    public Object registerUser(@Valid @RequestBody User user) {
        return userService.registerUser(user);
        // Now returns RegisterResponse (userId + walletId + name + email)
        // instead of just the User object
    }

    @PutMapping("/users/{id}")
    public Object updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/users/{id}")
    public Object deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
}