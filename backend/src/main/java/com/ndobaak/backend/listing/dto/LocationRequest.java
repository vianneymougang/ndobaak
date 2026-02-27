package com.ndobaak.backend.listing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationRequest {

    private String address;
    private Double latitude;
    private Double longitude;
    private String neighborhood;
    private String city;
}
