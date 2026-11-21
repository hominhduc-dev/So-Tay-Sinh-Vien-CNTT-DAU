package vn.tangthu.controller;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.tangthu.exception.ApiException;
import vn.tangthu.dto.PageResponse;
import vn.tangthu.dto.UpdateUserRequest;
import vn.tangthu.dto.UserDto;
import vn.tangthu.security.AuthUser;
import vn.tangthu.service.AdminService;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    /** UC14 */
    @GetMapping
    public PageResponse<UserDto> list(@RequestParam(required = false) @Size(max = 200) String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        if (q != null && q.length() > 200) {
            throw ApiException.validation("q tối đa 200 ký tự.");
        }
        return admin.search(q, PageResponse.request(page, size, Sort.by("code")));
    }

    /** UC15, UC16 */
    @PatchMapping("/{id}")
    public UserDto update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                          @RequestBody UpdateUserRequest req) {
        return admin.update(me.id(), id, req.active(), req.role());
    }
}
