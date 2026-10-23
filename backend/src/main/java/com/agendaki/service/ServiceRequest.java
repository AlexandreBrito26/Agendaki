package com.agendaki.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank(message = "Informe o nome do serviço")
        @Size(max = 100, message = "Nome muito longo")
        String name,

        @NotNull(message = "Informe a duração em minutos")
        @Min(value = 5, message = "A duração mínima é 5 minutos")
        @Max(value = 600, message = "A duração máxima é 600 minutos")
        Integer durationMin,

        @NotNull(message = "Informe o preço em centavos")
        @Min(value = 0, message = "O preço não pode ser negativo")
        Integer priceCents,

        /** Opcional: usado no PUT para reativar/desativar. */
        Boolean active) {}
