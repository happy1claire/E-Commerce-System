package com.cs6650.shoppingcartservice.model;

// You must create this class
public class AuthorizationRequest {

    // The name should match the JSON property "credit_card_number".
    // Using @JsonProperty is good practice if the names differ (e.g.,
    // creditCardNumber).
    private String credit_card_number;

    // Default constructor (needed for deserialization, though not used here)
    public AuthorizationRequest() {
    }

    // Constructor to create the object easily
    public AuthorizationRequest(String creditCardNumber) {
        this.credit_card_number = creditCardNumber;
    }

    // Getter (needed for serialization)
    public String getCredit_card_number() {
        return credit_card_number;
    }

    // Setter (needed for deserialization)
    public void setCredit_card_number(String creditCardNumber) {
        this.credit_card_number = creditCardNumber;
    }
}