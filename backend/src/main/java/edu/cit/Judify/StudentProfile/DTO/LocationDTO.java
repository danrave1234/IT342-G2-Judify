package edu.cit.Judify.StudentProfile.DTO;

public class LocationDTO {
    private String city;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;

    // Default constructor
    public LocationDTO() {
    }

    // Constructor with parameters
    public LocationDTO(String city, String state, String country, Double latitude, Double longitude) {
        this.city = city;
        this.state = state;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
} 