package com.locadora.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Item item;

    private Integer quantidade;

    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private Integer renovacoes = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private StatusEmprestimo status; // ACTIVE, RETURNED, LATE, BLOCKED

    @Column(nullable = false)
    private Double multa = 0.0;

    public enum StatusEmprestimo {

        ACTIVE("ATIVO"),
        RETURNED("DEVOLVIDO"),
        LATE("ATRASADO"),
        BLOCKED("BLOQUEADO");

        private final String label;

        StatusEmprestimo(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        // Permite converter "DEVOLVIDO" -> RETURNED
        public static StatusEmprestimo fromLabel(String label) {
            for (StatusEmprestimo s : values()) {
                if (s.label.equalsIgnoreCase(label)) {
                    return s;
                }
            }
            // Caso o request venha com texto inválido
            throw new IllegalArgumentException("Status inválido: " + label);
        }
    }
}
