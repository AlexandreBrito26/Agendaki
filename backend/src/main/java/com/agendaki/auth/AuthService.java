package com.agendaki.auth;

import com.agendaki.business.Business;
import com.agendaki.business.BusinessRepository;
import com.agendaki.config.ApiException;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    /** Slugs que colidiriam com rotas do frontend (a página pública fica em /{slug}). */
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "api", "admin", "app", "login", "entrar", "register", "cadastro", "painel",
            "dashboard", "agendamento", "agendamentos", "appointments", "public", "assets", "static");

    private final BusinessRepository businesses;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(BusinessRepository businesses, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.businesses = businesses;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String slug = req.slug().trim();

        if (RESERVED_SLUGS.contains(slug)) {
            throw ApiException.conflict("Este endereço (slug) é reservado, escolha outro");
        }
        if (businesses.existsByOwnerEmail(email)) {
            throw ApiException.conflict("Já existe uma conta com este e-mail");
        }
        if (businesses.existsBySlug(slug)) {
            throw ApiException.conflict("Este endereço (slug) já está em uso");
        }

        Business business = new Business(
                req.businessName().trim(), slug, email, passwordEncoder.encode(req.password()));
        try {
            business = businesses.saveAndFlush(business);
        } catch (DataIntegrityViolationException e) {
            // corrida entre duas requisições com o mesmo e-mail/slug
            throw ApiException.conflict("E-mail ou slug já cadastrado");
        }
        return toResponse(business);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Business business = businesses.findByOwnerEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), business.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        return toResponse(business);
    }

    private AuthResponse toResponse(Business b) {
        return new AuthResponse(jwtService.generate(b.getId()), b.getId(), b.getName(), b.getSlug());
    }
}
