package com.teamEWSN.gitdeun.meeting.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class ParticipantId implements Serializable {
    private Long user;
    private Long meeting;
}