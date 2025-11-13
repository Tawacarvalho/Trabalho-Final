package com.locadora.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmprestimoRequest {
    private Long usuarioId;
    private Long itemId;
    private Integer quantidade;
    private String dataPrevistaDevolucao; // yyyy-MM-dd
}
