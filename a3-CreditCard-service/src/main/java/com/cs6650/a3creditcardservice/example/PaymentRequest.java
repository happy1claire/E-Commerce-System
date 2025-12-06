package com.cs6650.a3creditcardservice.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class PaymentRequest {
    @JsonProperty("credit_card_number")
    private String creditCardNumber;
}
