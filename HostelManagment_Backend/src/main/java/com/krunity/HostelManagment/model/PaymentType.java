package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "payment_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "type_id", updatable = false, nullable = false)
    private UUID typeId;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;
}
