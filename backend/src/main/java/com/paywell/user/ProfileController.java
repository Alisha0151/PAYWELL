package com.paywell.user;

import com.paywell.common.PageResponse;
import com.paywell.security.CurrentUser;
import com.paywell.user.ProfileDtos.ActivityView;
import com.paywell.user.ProfileDtos.AddPaymentMethodRequest;
import com.paywell.user.ProfileDtos.ChangePasswordRequest;
import com.paywell.user.ProfileDtos.PaymentMethodView;
import com.paywell.user.ProfileDtos.ProfileView;
import com.paywell.user.ProfileDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileView get() { return service.get(CurrentUser.id()); }

    @PatchMapping
    public ProfileView update(@Valid @RequestBody UpdateProfileRequest r) { return service.update(CurrentUser.id(), r); }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest r) { service.changePassword(CurrentUser.id(), r); }

    /** The caller's own security/audit activity (read-only). */
    @GetMapping("/activity")
    public PageResponse<ActivityView> activity(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.activity(CurrentUser.id(), page, size);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodView> methods() { return service.methods(CurrentUser.id()); }

    @PostMapping("/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodView addMethod(@Valid @RequestBody AddPaymentMethodRequest r) { return service.addMethod(CurrentUser.id(), r); }

    @DeleteMapping("/payment-methods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMethod(@PathVariable UUID id) { service.deleteMethod(CurrentUser.id(), id); }
}
