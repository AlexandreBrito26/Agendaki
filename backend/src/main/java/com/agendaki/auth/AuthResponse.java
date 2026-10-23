package com.agendaki.auth;

public record AuthResponse(String token, Long businessId, String businessName, String slug) {}
