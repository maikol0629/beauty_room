package com.mr.sb.beauty_room.services.implement;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.mr.sb.beauty_room.config.TelegramBotProperties;
import com.mr.sb.beauty_room.services.IQrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImplement implements IQrCodeService {

    private final TelegramBotProperties telegramBotProperties;

    @Override
    public byte[] generatePng(String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalArgumentException("No se pudo generar el código QR", e);
        }
    }

    @Override
    public String buildPublicAgendaUrl(String tenantKey) {
        String username = telegramBotProperties.getUsername();
        if (username == null || username.isBlank()) {
            return null;
        }
        return "https://t.me/" + username + "?start=" + tenantKey;
    }
}
