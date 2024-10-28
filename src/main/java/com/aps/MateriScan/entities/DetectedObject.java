package com.aps.MateriScan.entities;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DetectedObject {
    String name;
    float confidence;
    Rect boundingBox;

    public DetectedObject(String name, float confidence, Rect boundingBox) {
        this.name = name;
        this.confidence = confidence;
        this.boundingBox = boundingBox;
    }

    // Calcula a área da caixa delimitadora
    public int getArea() {
        return boundingBox.width * boundingBox.height;
    }

    public String getFormattedConfidence() {
        // Retorna a confiança formatada como percentual
        return String.format("%.2f%%", confidence * 100);
    }

    // Calcula a interseção sobre a união (IoU) entre duas caixas
    public float calculateIoU(DetectedObject other) {
        int x1 = Math.max(this.boundingBox.x, other.boundingBox.x);
        int y1 = Math.max(this.boundingBox.y, other.boundingBox.y);
        int x2 = Math.min(this.boundingBox.x + this.boundingBox.width, other.boundingBox.x + other.boundingBox.width);
        int y2 = Math.min(this.boundingBox.y + this.boundingBox.height, other.boundingBox.y + other.boundingBox.height);

        int intersectionArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        int unionArea = this.getArea() + other.getArea() - intersectionArea;

        return (float) intersectionArea / unionArea;
    }

    public String getName() {
        return name;
    }

    public float getConfidence() {
        return confidence;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }
}


