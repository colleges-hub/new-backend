package ru.ncti.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.ncti.backend.model.User;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void startup() {
        User user = new User();
        user.setFirstname("a");
        user.setLastname("a");
        user.setEmail("admin@gmail.com");

        userRepository.save(user);
    }

    @Test
    void findByEmail_shouldBeNotNull() {
        User candidate = userRepository.findByEmail("admin@gmail.com").orElse(null);

        assertNotNull(candidate);
    }

    @Test
    void findByEmail_shouldBeNull() {
        User candidate = userRepository.findByEmail("admin1@gmail.com").orElse(null);

        assertNull(candidate);
    }

    @Test
    void findByLastnameAndFirstname_shouldBeNotNull() {
        User candidate = userRepository.findByLastnameAndFirstname("a", "a").orElse(null);

        assertNotNull(candidate);
    }


    @Test
    void findByLastnameAndFirstname_shouldBeNull() {
        User candidate = userRepository.findByLastnameAndFirstname("a", "b").orElse(null);

        assertNull(candidate);
    }

}