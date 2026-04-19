package com.esg.cidadesinteligentes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "indicadores_esg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicadorESG {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String cidade;

    @NotBlank
    @Column(nullable = false)
    private String categoria; // AMBIENTAL, SOCIAL, GOVERNANCA

    @NotBlank
    @Column(nullable = false)
    private String indicador;

    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double valor;

    private String unidade;

    private Integer ano;
}
