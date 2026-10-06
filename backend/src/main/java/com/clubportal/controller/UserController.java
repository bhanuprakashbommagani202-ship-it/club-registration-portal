package com.clubportal.controller;

import com.clubportal.dto.CreateUserRequest;
import com.clubportal.dto.UserDto;
import com.clubportal.dto.UserStatusRequest;
import com.clubportal.model.Role;
import com.clubportal.model.User;
import com.clubportal.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list(@RequestParam(name = "role", required = false) Role role) {
        return userService.list(role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @PatchMapping("/{id}/status")
    public UserDto setActive(@AuthenticationPrincipal User admin, @PathVariable("id") Long id,
                             @Valid @RequestBody UserStatusRequest req) {
        return userService.setActive(id, req.active(), admin);
    }
}
