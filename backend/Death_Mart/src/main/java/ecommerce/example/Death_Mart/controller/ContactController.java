package ecommerce.example.Death_Mart.controller;

import ecommerce.example.Death_Mart.dto.request.ContactRequest;
import ecommerce.example.Death_Mart.dto.response.ApiResponse;
import ecommerce.example.Death_Mart.dto.response.ContactMessageResponse;
import ecommerce.example.Death_Mart.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitMessage(
            @Valid @RequestBody ContactRequest request) {
        ContactMessageResponse response = contactService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Your message has been sent successfully. We will get back to you soon.", response));
    }
}
