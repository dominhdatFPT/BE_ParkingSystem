package com.swp.parking.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import com.swp.parking.config.EkycProperties;
import com.swp.parking.dto.ekyc.EkycCccdResult;
import com.swp.parking.dto.ekyc.EkycLicenseResult;
import com.swp.parking.dto.ekyc.EkycValidationResult;
import com.swp.parking.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EkycService {

    private final EkycProperties ekycProperties;
    private final RestTemplate restTemplate;

    public EkycCccdResult ocrCccd(String base64Image) {
        if (isMockProvider()) {
            return new EkycCccdResult(
                    mockNationalId(),
                    "NGUYEN VAN A",
                    "2000-01-01",
                    "Nam",
                    "Viet Nam",
                    "TP HCM",
                    "TP HCM",
                    "2026-06-16",
                    "2036-06-16",
                    98.0
            );
        }

        if (isGoogleVisionProvider()) {
            String text = detectText(base64Image, "CCCD OCR");
            return new EkycCccdResult(
                    extractNationalId(text),
                    extractFullName(text),
                    extractDate(text),
                    extractGender(text),
                    extractNationality(text),
                    null,
                    null,
                    null,
                    null,
                    calculateConfidence(text)
            );
        }

        String url = ekycProperties.getBaseUrl() + "/v1/ocr/id-card";

        try {
            return restTemplate.postForObject(url, buildHttpEntity(base64Image), EkycCccdResult.class);
        } catch (Exception ex) {
            log.error("eKYC CCCD OCR request failed: {}", ex.getMessage(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY, "eKYC service unavailable: CCCD OCR");
        }
    }

    public EkycLicenseResult ocrLicense(String base64Image) {
        if (isMockProvider()) {
            return new EkycLicenseResult(
                    mockLicenseNumber(),
                    "NGUYEN VAN A",
                    "2000-01-01",
                    "B2",
                    "2026-06-16",
                    "2036-06-16",
                    "So Giao Thong Van Tai TP HCM"
            );
        }

        if (isGoogleVisionProvider()) {
            String text = detectText(base64Image, "driving license OCR");
            return new EkycLicenseResult(
                    extractLicenseNumber(text),
                    extractFullName(text),
                    extractDate(text),
                    extractLicenseClass(text),
                    null,
                    null,
                    extractIssuingAuthority(text)
            );
        }

        String url = ekycProperties.getBaseUrl() + "/v1/ocr/driving-license";

        try {
            return restTemplate.postForObject(url, buildHttpEntity(base64Image), EkycLicenseResult.class);
        } catch (Exception ex) {
            log.error("eKYC driving license OCR request failed: {}", ex.getMessage(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY, "eKYC service unavailable: driving license OCR");
        }
    }

    public String ocrLicensePlate(String base64Image) {
        if (isMockProvider()) {
            return "51F-12345";
        }
        log.info("Running license plate OCR with provider={}, path={}",
                ekycProperties.getProvider(), ekycProperties.getLicensePlatePath());

        String text = isGoogleVisionProvider()
                ? detectText(base64Image, "license plate OCR")
                : detectTextWithProvider(base64Image, "license plate OCR", ekycProperties.getLicensePlatePath());

        text = text.toUpperCase().replaceAll("[^A-Z0-9]", "");
        Matcher matcher = Pattern.compile("\\d{2}[A-Z]{1,2}\\d{4,5}").matcher(text);
        if (!matcher.find()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không đọc được biển số từ ảnh. Vui lòng chụp chính diện và đủ sáng");
        }

        String compact = matcher.group();
        int prefixLength = compact.length() - 5;
        return compact.substring(0, prefixLength) + "-" + compact.substring(prefixLength);
    }

    @SuppressWarnings("unchecked")
    private String detectTextWithProvider(String base64Image, String action, String path) {
        String effectivePath = path == null || path.isBlank() ? "/v1/ocr/license-plate" : path.trim();
        String normalizedPath = effectivePath.startsWith("/") ? effectivePath : "/" + effectivePath;
        String url = ekycProperties.getBaseUrl() + normalizedPath;

        try {
            Map<String, Object> response = restTemplate.postForObject(url, buildHttpEntity(base64Image), Map.class);
            String text = findPlateCandidate(response);
            return text != null ? text : "";
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("eKYC {} request failed: {}", action, ex.getMessage(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    "eKYC service unavailable: " + action + ". Kiem tra endpoint " + normalizedPath);
        }
    }

    private String findPlateCandidate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            Matcher matcher = Pattern.compile("\\d{2}\\s*[A-Z]{1,2}\\s*[-.]?\\s*\\d{4,5}", Pattern.CASE_INSENSITIVE)
                    .matcher(text);
            return matcher.find() ? matcher.group() : null;
        }
        if (value instanceof Map<?, ?> map) {
            for (String key : List.of("licensePlate", "license_plate", "plate", "plateNumber", "number", "text", "value")) {
                if (map.containsKey(key)) {
                    String candidate = findPlateCandidate(map.get(key));
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
            for (Object nested : map.values()) {
                String candidate = findPlateCandidate(nested);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        if (value instanceof Collection<?> collection) {
            for (Object nested : collection) {
                String candidate = findPlateCandidate(nested);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public String ocrVehicleDocument(String base64Image) {
        if (isMockProvider()) {
            return "GIAY CHUNG NHAN DANG KY XE 51F-12345 NGUYEN VAN A";
        }
        if (!isGoogleVisionProvider()) {
            return "";
        }
        return detectText(base64Image, "vehicle registration OCR");
    }

    public EkycValidationResult validateDocument(String base64Image) {
        if (isMockProvider()) {
            return new EkycValidationResult(true, false, "MOCK_EKYC", 98.0);
        }

        if (isGoogleVisionProvider()) {
            String text = detectText(base64Image, "document validation");
            Double confidence = calculateConfidence(text);
            boolean valid = confidence >= 70;
            return new EkycValidationResult(valid, false, "GOOGLE_VISION_OCR", confidence);
        }

        String url = ekycProperties.getBaseUrl() + "/v1/verify/document";

        try {
            return restTemplate.postForObject(url, buildHttpEntity(base64Image), EkycValidationResult.class);
        } catch (Exception ex) {
            log.error("eKYC document validation request failed: {}", ex.getMessage(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY, "eKYC service unavailable: document validation");
        }
    }

    private HttpEntity<Map<String, String>> buildHttpEntity(String base64Image) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ekycProperties.getToken());
        headers.set("TokenId", ekycProperties.getTokenId());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(Map.of("img", base64Image), headers);
    }

    private boolean isGoogleVisionProvider() {
        return "google-vision".equalsIgnoreCase(ekycProperties.getProvider());
    }

    private boolean isMockProvider() {
        return "mock".equalsIgnoreCase(ekycProperties.getProvider());
    }

    private Long currentMockUserId() {
        try {
            return (Long) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
        } catch (Exception ex) {
            return null;
        }
    }

    private String mockNumericId(String prefix, int totalLength) {
        Long userId = currentMockUserId();
        String value = prefix + (userId != null ? userId : 0L);
        if (value.length() > totalLength) {
            return value.substring(0, totalLength);
        }
        return value + "0".repeat(totalLength - value.length());
    }

    private String mockNationalId() {
        return mockNumericId("099", 12);
    }

    private String mockLicenseNumber() {
        return mockNumericId("079", 12);
    }

    private String detectText(String base64Image, String action) {
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            ByteString imageBytes = ByteString.copyFrom(decodeBase64Image(base64Image));
            Image image = Image.newBuilder().setContent(imageBytes).build();
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            AnnotateImageResponse imageResponse = response.getResponses(0);
            if (imageResponse.hasError()) {
                log.error("Google Vision {} failed: {}", action, imageResponse.getError().getMessage());
                throw new AppException(HttpStatus.BAD_GATEWAY, "Google Vision OCR failed: " + action);
            }

            String text = imageResponse.getFullTextAnnotation().getText();
            if ((text == null || text.isBlank()) && imageResponse.getTextAnnotationsCount() > 0) {
                text = imageResponse.getTextAnnotations(0).getDescription();
            }
            return text != null ? text.trim() : "";
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Google Vision {} request failed: {}", action, ex.getMessage(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY, "Google Vision OCR unavailable: " + action);
        }
    }

    private byte[] decodeBase64Image(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ảnh OCR không hợp lệ");
        }

        String normalized = base64Image.trim();
        int commaIndex = normalized.indexOf(',');
        if (normalized.startsWith("data:") && commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }
        normalized = normalized.replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private Double calculateConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        int usefulLength = text.replaceAll("\\s+", "").length();
        if (usefulLength >= 80) {
            return 92.0;
        }
        if (usefulLength >= 30) {
            return 82.0;
        }
        return 60.0;
    }

    private String extractNationalId(String text) {
        return firstMatch(text, "\\b\\d{12}\\b", "\\b\\d{9}\\b");
    }

    private String extractLicenseNumber(String text) {
        return firstMatch(text, "\\b\\d{10,12}\\b", "\\b[A-Z]{1,3}\\d{6,10}\\b");
    }

    private String extractDate(String text) {
        String date = firstMatch(text, "\\b\\d{2}/\\d{2}/\\d{4}\\b", "\\b\\d{2}-\\d{2}-\\d{4}\\b", "\\b\\d{4}-\\d{2}-\\d{2}\\b");
        if (date == null) {
            return null;
        }
        if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return date;
        }
        String[] parts = date.split("[/-]");
        return parts[2] + "-" + parts[1] + "-" + parts[0];
    }

    private String extractGender(String text) {
        String normalized = normalize(text);
        if (normalized.contains(" gioi tinh nam") || normalized.contains(" sex nam") || normalized.contains(" male")) {
            return "Nam";
        }
        if (normalized.contains(" gioi tinh nu") || normalized.contains(" sex nu") || normalized.contains(" female")) {
            return "Nữ";
        }
        return null;
    }

    private String extractNationality(String text) {
        String normalized = normalize(text);
        if (normalized.contains("viet nam") || normalized.contains("vietnam")) {
            return "Việt Nam";
        }
        return null;
    }

    private String extractLicenseClass(String text) {
        Matcher matcher = Pattern.compile("(?i)(?:hang|class|hạng)\\s*[:\\-]?\\s*([A-Z][0-9]?)").matcher(text);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private String extractIssuingAuthority(String text) {
        for (String line : text.split("\\R")) {
            String normalized = normalize(line);
            if (normalized.contains("so giao thong") || normalized.contains("cuc duong bo") || normalized.contains("authority")) {
                return line.trim();
            }
        }
        return null;
    }

    private String extractFullName(String text) {
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String normalized = normalize(lines[i]);
            if ((normalized.contains("ho va ten") || normalized.contains("full name") || normalized.equals("name")) && i + 1 < lines.length) {
                String candidate = cleanName(lines[i + 1]);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        for (String line : lines) {
            String candidate = cleanName(line);
            if (candidate != null && candidate.split("\\s+").length >= 2) {
                return candidate;
            }
        }
        return null;
    }

    private String cleanName(String line) {
        if (line == null) {
            return null;
        }
        String cleaned = line.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() < 5) {
            return null;
        }
        String normalized = normalize(cleaned);
        if (normalized.contains("cong hoa")
                || normalized.contains("can cuoc")
                || normalized.contains("giay phep")
                || normalized.contains("ngay sinh")
                || normalized.contains("date")
                || normalized.contains("nationality")
                || normalized.contains("license")) {
            return null;
        }
        return cleaned;
    }

    private String firstMatch(String text, String... patterns) {
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
