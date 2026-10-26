package com.parlour.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Single-row table holding the editable website content. */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
public class SiteSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String parlourName;

    @Column(nullable = false)
    private String tagline;

    @Column(length = 2000)
    private String about;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 500)
    private String openingHours;

    @Column(length = 500)
    private String offerText;

    @Column(length = 500)
    private String heroImageUrl;
}
