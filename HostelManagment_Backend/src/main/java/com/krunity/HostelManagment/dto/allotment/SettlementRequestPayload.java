package com.krunity.HostelManagment.dto.allotment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class SettlementRequestPayload {

    /** Desired last day in the room. Must be today or later. */
    @FutureOrPresent(message = "Requested end date must be today or in the future")
    private LocalDate requestedEndDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
