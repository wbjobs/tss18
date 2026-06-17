package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateNode {

    private String id;

    private String name;

    private String type;

    private Integer x;

    private Integer y;

    private String color;

    private List<String> permissions;
}
