package com.auth.service;

import com.auth.dto.UserDto;
import com.auth.entity.Provider;
import com.auth.entity.User;
import com.auth.exceptions.ResourceNotFoundException;
import com.auth.mapper.UserMapper;
import com.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = userMapper.mapToUser(userDto);
//        user.setId(UUID.randomUUID());
        user.setProvider(Provider.LOCAL);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        log.info("User created successfully!");
        return userMapper.mapToUserDto(savedUser);
    }

    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(userMapper::mapToUserDto)
                .toList();
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id."));
        return userMapper.mapToUserDto(user);
    }

    ;

    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id."));
        return userMapper.mapToUserDto(user);
    }

    public UserDto updateUser(UserDto userDto, UUID userId) {

        User existingUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id."));

        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getGender() != null) existingUser.setGender(userDto.getGender());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());

        if (userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnabled(existingUser.isEnabled());
        User updatedUser = userRepository.save(existingUser);
        return userMapper.mapToUserDto(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id."));
        userRepository.delete(user);
    }
}
