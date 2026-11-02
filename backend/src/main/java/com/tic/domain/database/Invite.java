package com.tic.domain.database;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "invites")
@Getter @Setter @NoArgsConstructor
public class Invite {
    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private Player invitedUser;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "request_user_id", nullable = false)
    private Player requestUser;

    private boolean pending = true;
    private boolean accepted = false;
}
