package com.teamEWSN.gitdeun.visithistory.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class VisitHistoryId implements Serializable {
    private Long user;
    private Long mindmap;
}
