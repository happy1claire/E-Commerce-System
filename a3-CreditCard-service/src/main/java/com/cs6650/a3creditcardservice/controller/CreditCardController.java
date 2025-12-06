package com.cs6650.a3creditcardservice.controller;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cs6650.a3creditcardservice.example.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;
@Slf4j
@RequestMapping("/credit-card-authorizer")
@RestController

public class CreditCardController {

    private static final Pattern CARD_PATTERN =
            Pattern.compile("^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$");

    private static final int AUTH_SUCCESS_PERCENT = 90;

    @PostMapping("/authorize")
    public ResponseEntity<String> authorize(@RequestBody PaymentRequest body){
        String cc = body == null ? null: body.getCreditCardNumber();

        //validate
        if(cc == null || !CARD_PATTERN.matcher(cc).matches()){
           log.info("Invalid credit card number");
           throw new ResponseStatusException(
                   HttpStatus.BAD_REQUEST, "Invalid credit card number"
           );
        }
        log.info("validate credit card number");

        // 90% Authorized, 10% Declined
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < AUTH_SUCCESS_PERCENT) {
            log.info("CCA AUTHORIZED {}", cc);
            // 規格允許 200 OK 無 body；若要帶訊息可回一個小物件
            return ResponseEntity.ok("Authorized");
        } else {
            log.info("CCA DECLINED {}", cc);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("PAYMENT_DECLINED");
        }

    }


}
