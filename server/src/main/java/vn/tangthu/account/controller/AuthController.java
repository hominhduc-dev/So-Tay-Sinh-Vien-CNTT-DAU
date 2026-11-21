package vn.tangthu.account.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tangthu.account.dto.ChangePasswordRequest;
import vn.tangthu.account.dto.LoginRequest;
import vn.tangthu.account.dto.UserDto;
import vn.tangthu.common.security.AuthUser;
import vn.tangthu.account.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** UC01 */
    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.login(), req.password());
    }

    /** UC03 */
    @GetMapping("/me")
    public Map<String, UserDto> me(@AuthenticationPrincipal AuthUser me) {
        return Map.of("user", auth.me(me.id()));
    }

    /** UC04 */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest req) {
        auth.changePassword(me.id(), req.currentPassword(), req.newPassword());
    }
}
