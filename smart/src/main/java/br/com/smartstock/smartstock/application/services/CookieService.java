package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.infra.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CookieService {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final Boolean secure;

    public CookieService(@Value("${jwtTokens.cookie.secure}") Boolean secure) {
        this.secure = secure;
    }

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        setCookie(response, ACCESS_TOKEN, token, 60 * 15);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        setCookie(response, REFRESH_TOKEN, token, 7 * 24 * 60 * 60);
    }

    public void clearCookies(HttpServletResponse response) {
        setCookie(response, ACCESS_TOKEN, "", 0);
        setCookie(response, REFRESH_TOKEN, "", 0);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extrairCookie(request, REFRESH_TOKEN);
    }

    private void setCookie(HttpServletResponse response, String nome, String valor, int maxAgeSegundos) {
        ResponseCookie cookie = ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Strict")
                .maxAge(maxAgeSegundos)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extrairCookie(HttpServletRequest request, String nome) {
        return CookieUtil.extrairCookie(request, nome);
    }
}
