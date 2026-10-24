package com.agendaki.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void celularComMascaraGanhaCodigoDoPais() {
        assertThat(PhoneNumbers.toE164("(11) 99999-8888", "55")).isEqualTo("5511999998888");
    }

    @Test
    void fixoComDdd() {
        assertThat(PhoneNumbers.toE164("11 3333-4444", "55")).isEqualTo("551133334444");
    }

    @Test
    void ddd55NaoConfundeComCodigoDoPais() {
        // Rio Grande do Sul: DDD 55 + 9 dígitos = 11 dígitos, é número nacional.
        assertThat(PhoneNumbers.toE164("55 99999-8888", "55")).isEqualTo("5555999998888");
    }

    @Test
    void numeroJaComCodigoDoPais() {
        assertThat(PhoneNumbers.toE164("5511999998888", "55")).isEqualTo("5511999998888");
        assertThat(PhoneNumbers.toE164("+55 11 99999-8888", "55")).isEqualTo("5511999998888");
    }

    @Test
    void zeroDeTroncoEPrefixoInternacional() {
        assertThat(PhoneNumbers.toE164("011 99999-8888", "55")).isEqualTo("5511999998888");
        assertThat(PhoneNumbers.toE164("0055 11 99999-8888", "55")).isEqualTo("5511999998888");
    }

    @Test
    void numeroInternacionalDeOutroPaisNaoRecebeCodigoBrasileiro() {
        assertThat(PhoneNumbers.toE164("+1 415 555 2671", "55")).isEqualTo("14155552671");
    }

    @Test
    void entradasInvalidasViramNull() {
        assertThat(PhoneNumbers.toE164(null, "55")).isNull();
        assertThat(PhoneNumbers.toE164("   ", "55")).isNull();
        assertThat(PhoneNumbers.toE164("12345", "55")).isNull();
        assertThat(PhoneNumbers.toE164("999998888", "55")).isNull();      // sem DDD
        assertThat(PhoneNumbers.toE164("+1234567890123456", "55")).isNull(); // mais de 15 dígitos
    }

    @Test
    void mascaraMostraSoOsUltimosQuatroDigitos() {
        assertThat(PhoneNumbers.mask("5511999998888")).isEqualTo("****8888");
        assertThat(PhoneNumbers.mask("12")).isEqualTo("****");
        assertThat(PhoneNumbers.mask(null)).isEqualTo("—");
    }
}
