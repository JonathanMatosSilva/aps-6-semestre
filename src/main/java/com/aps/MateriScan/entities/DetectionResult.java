package com.aps.MateriScan.entities;

import java.util.List;

public class DetectionResult {
    private String imageBase64; // Mude de byte[] para String
    private List<DetectedObject> detectedObjects;

    public DetectionResult(String imageBase64, List<DetectedObject> detectedObjects) {
        this.imageBase64 = imageBase64;
        this.detectedObjects = detectedObjects;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }
}



