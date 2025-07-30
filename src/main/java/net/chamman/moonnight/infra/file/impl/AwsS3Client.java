package net.chamman.moonnight.infra.file.impl;

import static net.chamman.moonnight.global.exception.HttpStatusCode.S3_DELETE_FAIL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.S3_UPLOAD_FAIL;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.infra.s3.S3DeleteException;
import net.chamman.moonnight.global.exception.infra.s3.S3UploadException;
import net.chamman.moonnight.infra.file.FileStorage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class AwsS3Client implements FileStorage{
	
	@Value("${aws.s3.bucket}")
	private String bucket;
	private final S3Client s3Client;


	/** 단일 파일 업로드
	 * @param file
	 * @param keyPrefix
	 * @throws S3UploadException {@link #uploadFile}
	 * @return
	 */
	@Override
	public String upload(MultipartFile file, String keyPrefix) {
		String key = keyPrefix + UUID.randomUUID() + "_" + file.getOriginalFilename(); // 고유한 파일명 생성
		log.debug("* S3 파일 업로드 요청. Bucket: [{}], Key: [{}]", bucket, key);

		try {
			
			// S3 업로드 요청 생성
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(file.getContentType())
					.acl("public-read")
					.build();
			
			// S3에 파일 업로드
			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
			
			// 업로드된 파일의 S3 URL 반환
			return "https://" + bucket + ".s3.amazonaws.com/" + key;
			
		} catch (Exception e) {
			throw new S3UploadException(S3_UPLOAD_FAIL, e);
		}
	}
	
	/** 경로들의 파일 삭제
	 * @param deleteFiles
	 * @throws S3DeleteException {@link #deleteFiles}
	 */
	@Override
	public void delete(List<String> paths) {

		if (paths == null || paths.isEmpty()) {
			return;
		}
		for(String s : paths) {
			log.debug("* S3 파일 삭제 요청. Bucket: [{}], Key: [{}]", bucket, s);
		}

		try {
			// S3 Object Key 리스트 변환
			List<ObjectIdentifier> objectIdentifiers = paths.stream()
					.map(path -> ObjectIdentifier.builder().key(path).build())
					.collect(Collectors.toList());
			
			// DeleteObjectsRequest 생성
			DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
					.bucket(bucket)
					.delete(builder -> builder.objects(objectIdentifiers))
					.build();
			
			s3Client.deleteObjects(deleteRequest);
		} catch (Exception e) {
			throw new S3DeleteException(S3_DELETE_FAIL, e);
		}
	}
	
}
