package com.agendaki.appointment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** date e time são no fuso horário do negócio. Ex.: "2026-10-25" e "14:30". */
public record BookingRequest(
        @NotNull(message = "Escolha o serviço") Long serviceId,
        @NotNull(message = "Escolha o profissional") Long professionalId,
        @NotNull(message = "Escolha o dia") LocalDate date,
        @NotNull(message = "Escolha o horário") LocalTime time,

        @NotBlank(message = "Informe seu nome")
        @Size(max = 100, message = "Nome muito longo")
        String customerName,

        @NotBlank(message = "Informe seu telefone")
        @Pattern(regexp = "^[0-9+()\\-\\s]{8,20}$", message = "Telefone inválido")
        String customerPhone,

        /** Opcional: se informado, o cliente recebe confirmação e lembrete por e-mail. */
        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail muito longo")
        String customerEmail) {}
