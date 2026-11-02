package io.dev.openspec.springbootopenspecapp.auth;

import io.dev.openspec.springbootopenspecapp.auth.dto.LoginRequest;
import io.dev.openspec.springbootopenspecapp.auth.dto.LoginResponse;
import io.dev.openspec.springbootopenspecapp.security.JwtService;
import io.dev.openspec.springbootopenspecapp.common.dto.ProblemDetailSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Emissão de tokens JWT")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Autentica e emite um token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "400", description = "Corpo inválido", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        String token = jwtService.generateToken(authentication.getName());
        return LoginResponse.bearer(token, jwtService.getExpiration().toSeconds());
    }
}
