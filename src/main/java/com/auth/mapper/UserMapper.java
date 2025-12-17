package com.auth.mapper;

import com.auth.dto.UserDto;
import com.auth.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User mapToUser(UserDto userDto);

    UserDto mapToUserDto(User user);
}
