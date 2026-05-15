package com.quickbite.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.PaymentStatusResponse;
import com.quickbite.payment.dto.RazorpayOrderRequest;
import com.quickbite.payment.dto.RazorpayOrderResponse;
import com.quickbite.payment.dto.RazorpayVerifyRequest;
import com.quickbite.payment.dto.RazorpayVerifyResponse;
import com.quickbite.payment.model.PaymentTransaction;
import com.quickbite.payment.repository.PaymentTransactionRepository;
import com.quickbite.payment.service.PaymentService;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final RestClient restClient;

    public PaymentServiceImpl(
            PaymentTransactionRepository paymentTransactionRepository,
            @Value("${app.razorpay.key-id}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret}") String razorpayKeySecret
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.razorpayKeyId = resolveKey("RAZORPAY_KEY_ID", razorpayKeyId);
        this.razorpayKeySecret = resolveKey("RAZORPAY_KEY_SECRET", razorpayKeySecret);
        this.restClient = RestClient.builder().build();
    }

    @Override
    public PaymentResponse charge(PaymentRequest request) {
        logger.info("Processing direct charge for orderReference={} paymentMethod={}", request.orderReference(), request.paymentMethod());
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(request.orderReference());
        transaction.setUserId(request.customerEmail());
        transaction.setTotalAmount(request.amount());
        transaction.setPaymentMode(normalizePaymentMode(request.paymentMethod()));
        transaction.setCurrency("INR");
        transaction.setPaymentStatus("SUCCESS");
        paymentTransactionRepository.save(transaction);
        logger.info("Direct charge recorded with SUCCESS status for orderReference={}", request.orderReference());

        return new PaymentResponse(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.orderReference(),
                request.paymentMethod(),
                request.amount(),
                "SUCCESS"
        );
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request) {
        logger.info("Creating Razorpay order for orderReference={} paymentMode={}", request.orderReference(), request.paymentMode());
        validateKeys();
        String paymentMode = normalizePaymentMode(request.paymentMode());
        if (!paymentMode.equals("UPI") && !paymentMode.equals("CARD")) {
            throw new IllegalArgumentException("paymentMode must be UPI or CARD");
        }
        if (!"INR".equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Only INR currency is supported");
        }

        long amountInPaise = Math.round(request.amount() * 100.0);
        if (amountInPaise <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        try {
            Map<String, Object> notes = new HashMap<>();
            notes.put("customerEmail", request.customerEmail());
            notes.put("paymentMode", paymentMode);
            if (request.notes() != null && !request.notes().isBlank()) {
                notes.put("notes", request.notes().trim());
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", amountInPaise);
            payload.put("currency", "INR");
            payload.put("receipt", request.orderReference());
            payload.put("notes", notes);

            JsonNode razorpayOrder = restClient.post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            if (razorpayOrder == null || razorpayOrder.path("id").asText().isBlank()) {
                throw new IllegalStateException("Razorpay order response is invalid");
            }

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setOrderId(request.orderReference());
            transaction.setUserId(request.customerEmail());
            transaction.setTotalAmount(request.amount());
            transaction.setPaymentMode(paymentMode);
            transaction.setPaymentStatus("PENDING");
            transaction.setRazorpayOrderId(razorpayOrder.path("id").asText());
            transaction.setCurrency("INR");
            paymentTransactionRepository.save(transaction);
            logger.info("Razorpay order created and pending transaction stored for orderReference={}", request.orderReference());

            return new RazorpayOrderResponse(
                    razorpayKeyId,
                    request.orderReference(),
                    razorpayOrder.path("id").asText(),
                    razorpayOrder.path("amount").asLong(),
                    razorpayOrder.path("currency").asText("INR"),
                    paymentMode,
                    razorpayOrder.path("status").asText("created")
            );
        } catch (Exception ex) {
            logger.error("Failed to create Razorpay order for orderReference={}", request.orderReference(), ex);
            throw new RuntimeException("Unable to create Razorpay order", ex);
        }
    }

    @Override
    public RazorpayVerifyResponse verifyRazorpayPayment(RazorpayVerifyRequest request) {
        logger.info("Verifying Razorpay payment for orderReference={}", request.orderReference());
        validateKeys();
        String paymentMode = normalizePaymentMode(request.paymentMode());
        boolean verified = isSignatureValid(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature(),
                razorpayKeySecret
        );

        PaymentTransaction transaction = paymentTransactionRepository
                .findTopByOrderIdOrderByIdDesc(request.orderReference())
                .orElseGet(PaymentTransaction::new);

        transaction.setOrderId(request.orderReference());
        transaction.setUserId(request.customerEmail());
        transaction.setTotalAmount(request.amount());
        transaction.setPaymentMode(paymentMode);
        transaction.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        transaction.setRazorpayOrderId(request.razorpayOrderId());
        transaction.setRazorpayPaymentId(request.razorpayPaymentId());
        transaction.setPaymentStatus(verified ? "SUCCESS" : "FAILED");
        paymentTransactionRepository.save(transaction);

        if (!verified) {
            logger.warn("Razorpay signature validation failed for orderReference={}", request.orderReference());
            throw new IllegalArgumentException("Invalid Razorpay signature");
        }

        logger.info("Razorpay payment verified successfully for orderReference={}", request.orderReference());

        return new RazorpayVerifyResponse(
                request.orderReference(),
                paymentMode,
                "SUCCESS",
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                true,
                "Payment verified successfully"
        );
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(String orderReference) {
        logger.debug("Fetching payment status for orderReference={}", orderReference);
        PaymentTransaction transaction = paymentTransactionRepository
                .findTopByOrderIdOrderByIdDesc(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found"));

        return new PaymentStatusResponse(
                transaction.getOrderId(),
                transaction.getPaymentMode(),
                transaction.getPaymentStatus(),
                transaction.getRazorpayOrderId(),
                transaction.getRazorpayPaymentId(),
                transaction.getCurrency(),
                transaction.getTotalAmount() == null ? 0.0 : transaction.getTotalAmount()
        );
    }

    private String normalizePaymentMode(String paymentMode) {
        String normalized = paymentMode == null ? "" : paymentMode.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("UPI")) {
            return "UPI";
        }
        if (normalized.contains("CARD") || normalized.contains("DEBIT") || normalized.contains("CREDIT")) {
            return "CARD";
        }
        return normalized;
    }

    private void validateKeys() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            logger.error("Razorpay key validation failed: credentials are missing");
            throw new IllegalStateException("Razorpay keys are not configured. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in payment-service/.env and restart payment-service.");
        }
    }

    private String resolveKey(String keyName, String injectedValue) {
        String normalizedInjected = normalizeKey(injectedValue);
        if (normalizedInjected != null) {
            return normalizedInjected;
        }

        String fromSystemProperty = normalizeKey(System.getProperty(keyName));
        if (fromSystemProperty != null) {
            return fromSystemProperty;
        }

        String fromEnv = normalizeKey(System.getenv(keyName));
        if (fromEnv != null) {
            return fromEnv;
        }

        Dotenv rootDotenv = Dotenv.configure().ignoreIfMissing().load();
        String fromRootDotenv = normalizeKey(rootDotenv.get(keyName));
        if (fromRootDotenv != null) {
            return fromRootDotenv;
        }

        Dotenv serviceDotenv = Dotenv.configure().directory("payment-service").ignoreIfMissing().load();
        return normalizeKey(serviceDotenv.get(keyName));
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private boolean isSignatureValid(String razorpayOrderId, String razorpayPaymentId, String signature, String secret) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = toHex(digest);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            logger.error("Failed to verify Razorpay signature due to internal error");
            throw new RuntimeException("Failed to verify Razorpay signature", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}

