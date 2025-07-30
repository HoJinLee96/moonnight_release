package net.chamman.moonnight.infra.file;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String upload(MultipartFile file, String keyPrefix); // 파일 업로드
    void delete(List<String> filePaths);             // 파일 삭제
}
