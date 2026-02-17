package com.denys.rag_assistant.controller;

import com.denys.rag_assistant.persistence.entity.Role;
import com.denys.rag_assistant.persistence.entity.UserEntity;
import com.denys.rag_assistant.service.data.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserEntity create(@RequestParam String name, @RequestParam Role role) {
        return userService.create(name, role);
    }

    @GetMapping
    public List<UserEntity> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserEntity getById(@PathVariable UUID id) {
        return userService.getById(id);
    }
}
