package com.example.texttosqlchat.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Sample JPA Entity representing a database table.
 * The schema derived from this entity is crucial for the Text-to-SQL prompt.
 */
@Entity
@Table(name = "product")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Categories like 'Electronics', 'Apparel', 'Books'
    private String category;

    private Double price;

    // Stock quantity
    private Integer stockQuantity;

    // NOTE: In a real application, you would also need a JpaRepository
    // (e.g., ProductRepository) to interact with this entity.

}

