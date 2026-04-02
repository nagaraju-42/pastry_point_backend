package com.bakeryq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String razorpayOrderId;
    private String razorpayKeyId;      // Public key sent to frontend
    private BigDecimal amount;         // In paise (multiply by 100)
    private String currency;
    private String receipt;
    private Long internalOrderId;
}
