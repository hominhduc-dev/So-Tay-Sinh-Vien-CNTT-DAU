package vn.tangthu.client.content.model;

public record DownloadedFile(String fileName, String mimeType, byte[] content) {}
