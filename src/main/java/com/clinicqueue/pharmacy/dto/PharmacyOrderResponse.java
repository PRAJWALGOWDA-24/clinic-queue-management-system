// pharmacy/dto/PharmacyOrderResponse.java
package com.clinicqueue.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PharmacyOrderResponse {
    private Long id;
    private String pharmacyName;
    private String status;
}