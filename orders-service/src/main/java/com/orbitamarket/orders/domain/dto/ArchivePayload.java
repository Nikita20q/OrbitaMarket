package com.orbitamarket.orders.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivePayload {

    @NotBlank(message = "AOI is required")
    @JsonProperty("aoi")
    private String aoi;

    @NotBlank(message = "Capture date is required")
    @JsonProperty("capture_date")
    private String captureDate;

    @NotBlank(message = "Sensor type is required")
    @JsonProperty("sensor_type")
    private String sensorType;
}