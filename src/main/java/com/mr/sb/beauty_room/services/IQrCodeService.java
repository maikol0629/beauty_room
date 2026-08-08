package com.mr.sb.beauty_room.services;

public interface IQrCodeService {

    byte[] generatePng(String content, int width, int height);

    String buildPublicAgendaUrl(String tenantKey);
}
