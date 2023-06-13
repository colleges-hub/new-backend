package ru.ncti.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 10-06-2023
 */
@Setter
@Getter
@Entity
@Builder
@Table(name = "private_chat")
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PrivateChat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    private User user1;

    @OneToOne
    private User user2;

    @OneToMany(mappedBy = "privateChat", cascade = {CascadeType.ALL})
    private List<Message> messages;

    public User getChatName(User currentUser) {
        if (currentUser.getEmail().equals(user1.getEmail())) {
            return user2;
        }
        return user1;
    }
}
