package dm.dracolich.user.web.mapper;

import dm.dracolich.user.dto.UserDto;
import dm.dracolich.user.dto.UserProfileDto;
import dm.dracolich.user.web.entity.UserEntity;
import dm.dracolich.user.web.entity.UserProfileEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.FIELD)
public interface UserMapper {
    UserDto entityToDto(UserEntity entity);
    @Mapping(target = "id", ignore = true)
    UserProfileDto profileEntityToDto(UserProfileEntity entity);

    UserProfileEntity dtoToProfileEntity(UserProfileDto dto);
}
