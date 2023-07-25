package ru.ncti.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Setter
@Getter
@Entity
@Builder
@Table(name = "private_chats")
@AllArgsConstructor
@NoArgsConstructor
public class PrivateChat {

    @Id
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