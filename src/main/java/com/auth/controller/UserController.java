package com.auth.controller;

import com.auth.dto.UserDto;
import com.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserDto userDto
    ) {
        UserDto user = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers() {
        Iterable<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmailId(
            @PathVariable String email) {
        UserDto userByEmail = userService.getUserByEmail(email);
        return ResponseEntity.ok(userByEmail);
    }

    @DeleteMapping("/{userID}")
    public void deleteUser(@PathVariable UUID userID) {
        userService.deleteUser(userID);
    }

    @PutMapping("/{userID}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID userID,
            @RequestBody UserDto userDto
    ) {
        UserDto updatedUser = userService.updateUser(userDto, userID);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{userID}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable UUID userID
    ) {
        return ResponseEntity.ok(userService.getUserById(userID));
    }

}
