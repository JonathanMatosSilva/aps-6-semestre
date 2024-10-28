package com.aps.MateriScan.controllers;

import com.aps.MateriScan.entities.DetectionResult;
import com.aps.MateriScan.services.VisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/image")
public class ImageController {

    @Autowired
    private VisionService visionService;

    @GetMapping("/upload")
    public String showUploadPage() {
        return "uploadPage"; // Nome da página HTML para upload
    }

    @PostMapping("/analyze")
    public String analyzeImage(@RequestParam("image") MultipartFile file, Model model) throws IOException {
        // Cria o recurso da imagem
        Resource imageResource = new ByteArrayResource(file.getBytes());

        // Analisa a imagem e obtém o resultado
        DetectionResult result = visionService.analyzeImage(imageResource);

        // Passa os dados para o modelo
        model.addAttribute("imageBase64", result.getImageBase64());
        model.addAttribute("detectedObjects", result.getDetectedObjects());

        return "resultPage"; // Nome do template Thymeleaf
    }
}
