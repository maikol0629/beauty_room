package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.config.WhatsAppProperties;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.services.implement.ChatUpdateHandler;
import com.mr.sb.beauty_room.services.implement.WhatsAppPayloadParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Webhook de WhatsApp (Meta Cloud API). GET valida el webhook en el dashboard de
 * Meta; POST recibe los mensajes (con firma X-Hub-Signature-256 verificada) y los
 * reenvía al ChatUpdateHandler como ChannelMessage.
 */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp", description = "Webhook de WhatsApp (Meta Cloud API)")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private static final String HUB_MODE_SUBSCRIBE = "subscribe";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final WhatsAppProperties properties;
    private final WhatsAppPayloadParser payloadParser;
    private final ChatUpdateHandler chatUpdateHandler;

    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Verificación del webhook", description = "Meta llama a este GET al configurar el webhook en el dashboard.")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        if (HUB_MODE_SUBSCRIBE.equals(mode) && properties.getVerifyToken() != null
                && properties.getVerifyToken().equals(verifyToken)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recibe mensajes de WhatsApp", description = "Meta hace POST acá con cada evento de mensaje.")
    public ResponseEntity<String> webhook(@RequestBody String body, HttpServletRequest request) {
        if (!isSignatureValid(body, request.getHeader(SIGNATURE_HEADER))) {
            log.warn("Firma X-Hub-Signature-256 inválida en webhook de WhatsApp. Se rechaza.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ChannelMessage> messages = payloadParser.parse(body);
        for (ChannelMessage msg : messages) {
            chatUpdateHandler.handleMessage(msg);
        }
        return ResponseEntity.ok("OK");
    }

    private boolean isSignatureValid(String body, String signatureHeader) {
        if (properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            log.warn("whatsapp.app-secret vacío: no se puede verificar la firma del webhook.");
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expectedHex = "sha256=" + HexFormat.of().formatHex(expected);
            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verificando firma del webhook de WhatsApp: {}", e.getMessage());
            return false;
        }
    }
}
