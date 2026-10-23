package com.agendaki.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ProfessionalRequest(
        @NotBlank(message = "Informe o nome do profissional")
        @Size(max = 100, message = "Nome muito longo")
        String name,

        /** Ids dos serviços que o profissional realiza. Vazio/nulo = nenhum. */
        Set<Long> serviceIds,

        /** Opcional: usado no PUT para reativar/desativar. */
        Boolean active) {}
