package com.cs6650.shoppingcartservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.cs6650.shoppingcartservice.model.AuthorizationRequest;

import jakarta.annotation.PostConstruct;

@Service
public class CreditCardAuthService {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceBaseUrl;

    private String authServiceUrl;

    public CreditCardAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        // Now authServiceBaseUrl is guaranteed to have its value
        this.authServiceUrl = "http://" + authServiceBaseUrl + "/credit-card-authorizer/authorize";
        System.out.println("Auth Service URL: " + this.authServiceUrl);
    }

    /**
     * Authorizes a credit card.
     * Throws PaymentDeclinedException if the card is declined (402).
     * Throws IllegalStateException for other network or server errors.
     */
    public void authorize(String creditCardNumber) {
        AuthorizationRequest request = new AuthorizationRequest(creditCardNumber);
        try {
            restTemplate.postForEntity(authServiceUrl, request, String.class);
            // If we get here, it was a 200-level success.

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) { // 402
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment was declined.");

            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) { // 400
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid payment information sent to auth service.");
            } else {
                // Other 4xx or 5xx errors
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authorization service failed.", e);
            }

        } catch (RestClientException e) {
            // Could not connect
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to authorization service.", e);
        }
    }
}