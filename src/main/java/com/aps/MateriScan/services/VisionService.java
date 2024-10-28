package com.aps.MateriScan.services;

import com.aps.MateriScan.entities.DetectedObject;
import com.aps.MateriScan.entities.DetectionResult;
import com.google.cloud.vision.v1.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import com.google.protobuf.ByteString;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class VisionService {

    static {
        System.loadLibrary("opencv_java490");
    }

    /**
     * Analisa a imagem fornecida e identifica objetos presentes nela.
     *
     * @param imageResource O recurso da imagem a ser analisada.
     * @return Mensagem indicando onde a imagem processada foi salva.
     * @throws IOException Se ocorrer um erro ao ler a imagem ou ao se conectar ao cliente.
     */
    public DetectionResult analyzeImage(Resource imageResource) throws IOException {
        byte[] imgBytesArray = readImageBytes(imageResource.getInputStream());
        ByteString imgBytes = ByteString.copyFrom(imgBytesArray);

        // Envia a imagem para a API
        List<AnnotateImageResponse> responses = sendImageToApi(imgBytes);

        // Decodifica a imagem diretamente a partir do array de bytes
        Mat originalImage = Imgcodecs.imdecode(new MatOfByte(imgBytesArray), Imgcodecs.IMREAD_COLOR);
        List<DetectedObject> detectedObjects = processResponses(responses, originalImage);

        // Converte a imagem processada para Base64
        String imageBase64 = encodeImageToBase64(originalImage);

        return new DetectionResult(imageBase64, detectedObjects);
    }

    private String encodeImageToBase64(Mat image) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte); // ou qualquer outro formato desejado
        byte[] byteArray = matOfByte.toArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }



    /**
     * Lê a imagem e a converte em um array de bytes.
     *
     */
    public byte[] readImageBytes(InputStream inputStream) throws IOException {
        return StreamUtils.copyToByteArray(inputStream);
    }


    /**
     * Envia a imagem para a API do Google Vision e obtém as respostas.
     *
     * @param imgBytes A imagem em bytes a ser enviada.
     * @return A lista de respostas da API.
     * @throws IOException Se ocorrer um erro ao conectar-se ao cliente.
     */
    private List<AnnotateImageResponse> sendImageToApi(ByteString imgBytes) throws IOException {
        Image img = Image.newBuilder().setContent(imgBytes).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
                .setImage(img)
                .build();

        // Inicializar o cliente
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            // Realizar a requisição
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
            return response.getResponsesList();
        }
    }

    /**
     * Processa as respostas da API e desenha os objetos detectados na imagem original.
     *
     * @param responses A lista de respostas da API.
     * @param originalImage A imagem original a ser processada.
     */
    private List<DetectedObject> processResponses(List<AnnotateImageResponse> responses, Mat originalImage) {
        List<DetectedObject> detections = new ArrayList<>();

        // Processa as respostas da API e coleta as detecções
        for (AnnotateImageResponse res : responses) {
            for (LocalizedObjectAnnotation entity : res.getLocalizedObjectAnnotationsList()) {
                List<NormalizedVertex> vertices = entity.getBoundingPoly().getNormalizedVerticesList();
                int x1 = (int) (vertices.get(0).getX() * originalImage.cols());
                int y1 = (int) (vertices.get(0).getY() * originalImage.rows());
                int x2 = (int) (vertices.get(2).getX() * originalImage.cols());
                int y2 = (int) (vertices.get(2).getY() * originalImage.rows());

                Rect boundingBox = new Rect(new Point(x1, y1), new Point(x2, y2));
                DetectedObject detectedObject = new DetectedObject(entity.getName(), entity.getScore(), boundingBox);
                detections.add(detectedObject);
            }
        }

        // Aplicar Non-Maximum Suppression (NMS)
        List<DetectedObject> filteredDetections = applyNMS(detections, 0.5f);

        // Desenhar retângulos e rótulos para as detecções filtradas
        for (DetectedObject detectedObject : filteredDetections) {
            Rect boundingBox = detectedObject.getBoundingBox();
            Imgproc.rectangle(originalImage, boundingBox.tl(), boundingBox.br(), new Scalar(0, 255, 0), 2); // Cor verde e espessura 2
            drawLabel(detectedObject.getName(), boundingBox, originalImage); // Desenhar o rótulo
        }

        return filteredDetections;
    }



    /**
     * Desenha o rótulo do objeto acima do retângulo desenhado.
     *
     * @param label O nome do objeto a ser desenhado.
     * @param boundingBox A caixa delimitadora do objeto.
     * @param originalImage A imagem onde o rótulo será desenhado.
     */
    private void drawLabel(String label, Rect boundingBox, Mat originalImage) {
        int[] baseline = new int[1];
        Size labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, 2, baseline); // Tamanho da fonte

        // Calcular a posição do texto
        Point labelPoint = new Point(boundingBox.tl().x, boundingBox.tl().y - 10); // Ajuste vertical

        // Verificar se o texto sai da imagem
        if (labelPoint.y < 0) {
            labelPoint.y = boundingBox.tl().y + boundingBox.height + labelSize.height + 10; // Mover para baixo se estiver fora
        }
        if (labelPoint.x + labelSize.width > originalImage.cols()) {
            labelPoint.x = originalImage.cols() - labelSize.width - 10; // Mover para esquerda se estiver fora
        }

        Imgproc.putText(originalImage, label, labelPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 0), 2); // Aumentar espessura
    }

    /**
     * Salva a imagem processada em um caminho específico.
     *
     * @param originalImage A imagem a ser salva.
     * @return Caminho da imagem salva.
     */
    private String saveProcessedImage(Mat originalImage) {
        String outputPath = "src/main/resources/static/images/imagem_processada.jpg";
        Imgcodecs.imwrite(outputPath, originalImage);
        return "/images/imagem_processada.jpg"; // Caminho relativo para ser usado na página HTML
    }

    private List<DetectedObject> applyNMS(List<DetectedObject> detections, float iouThreshold) {
        List<DetectedObject> result = new ArrayList<>();

        // Ordena pela confiança de forma decrescente
        detections.sort(Comparator.comparing(DetectedObject::getConfidence).reversed());

        while (!detections.isEmpty()) {
            DetectedObject best = detections.remove(0);
            result.add(best);

            detections.removeIf(det -> det.calculateIoU(best) > iouThreshold);
        }

        return result;
    }

}
