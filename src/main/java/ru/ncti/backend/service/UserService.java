package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.dto.ChangePasswordDTO;
import ru.ncti.backend.dto.UserViewDTO;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.RoleRepository;
import ru.ncti.backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Log4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public String changePassword(ChangePasswordDTO dto) throws IllegalArgumentException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (dto.getPassword() == null || dto.getPassword().length() <= 5) {
            throw new IllegalArgumentException("Не удалось поменять пароль");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        return "Пароль успешно изменен";
    }

    public List<UserViewDTO> getUsers(String type) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        final List<UserViewDTO> users = new ArrayList<>();

        if (type == null) {
            userRepository.findAll().forEach(user -> {
                if (!user.getId().equals(currentUser.getId()) && user.getId() != 1) {
                    users.add(UserViewDTO.builder()
                            .id(user.getId())
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .surname(user.getSurname())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .build());
                }
            });
        }
//        else if (type.equals("student")) {
//            roleRepository.findByName("ROLE_STUDENT")
//                    .ifPresent(role -> {
//                        userRepository
//                                .findAllByRolesIn(Set.of(role))
//                                .forEach(s -> users.add(UserDTO.builder()
//                                        .id(s.getId())
//                                        .firstname(s.getFirstname())
//                                        .lastname(s.getLastname())
//                                        .surname(s.getSurname())
//                                        .email(s.getEmail())
//                                        .username(s.getUsername())
//                                        .build()));
//                    });
//        } else if (type.equals("teacher")) {
//            roleRepository.findByName("ROLE_TEACHER")
//                    .ifPresent(role -> {
//                        userRepository.findAllByRolesIn(Set.of(role))
//                                .forEach(s -> users.add(UserDTO.builder()
//                                        .id(s.getId())
//                                        .firstname(s.getFirstname())
//                                        .lastname(s.getLastname())
//                                        .surname(s.getSurname())
//                                        .email(s.getEmail())
//                                        .username(s.getUsername())
//                                        .build()));
//                    });
//        }

        return users;
    }

}
