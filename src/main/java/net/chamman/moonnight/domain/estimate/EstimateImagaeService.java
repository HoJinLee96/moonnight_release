package net.chamman.moonnight.domain.estimate;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.exception.infra.s3.S3DeleteException;
import net.chamman.moonnight.global.exception.infra.s3.S3UploadException;
import net.chamman.moonnight.infra.file.FileStorage;
import net.chamman.moonnight.infra.file.impl.AwsS3Client;

@Service
@RequiredArgsConstructor
public class EstimateImagaeService {

	@Value("${aws.s3.bucket}")
	String s3Bucket;

	String s3Url;

	@PostConstruct
	private void initSmsPath() {
		this.s3Url = "https://" + s3Bucket + ".s3.amazonaws.com/";
	}

	private final FileStorage fileStorage;

	/**
	 * S3에 견적서 관련 이미지들을 업로드한다. 업로드 시 파일 경로는 "estimateImages/yyyyMMddHHmmss_전화번호/파일명"
	 * 형식으로 생성된다.
	 *
	 * @param images 업로드할 {@link MultipartFile} 객체 리스트. 각 파일은 이미지여야 한다.
	 * @param phone  사용자 전화번호. S3 경로 생성에 사용되며, 하이픈(-)은 추후 제거된다.
	 * @return 업로드 성공 시 각 이미지의 S3 URL {@link List}. 업로드 실패 시 S3UploadException 발생.
	 * @throws S3UploadException {@link AwsS3Client#uploadFiles(List, String)} AWS
	 *                           S3에 파일 업로드 중 오류 발생 시.
	 */
	public List<String> uploadEstimateImages(List<MultipartFile> images, String estimateId) {

		String keyPrefix = "estimateImages/" + estimateId + "/";

		return images.stream().map(file -> fileStorage.upload(file, keyPrefix)).collect(Collectors.toList());
	}

	/**
	 * 지정된 S3 경로에 있는 견적서 이미지들을 삭제한다. 삭제 대상 경로는 반드시 "estimateImages/" 로 시작해야 한다.
	 *
	 * @param imagesPath 삭제할 이미지들의 S3 경로 {@link List}. 각 경로는 S3 객체 키여야 한다.
	 * @throws S3DeleteException     {@link AwsS3Client#deleteFiles(String[])} AWS
	 *                               S3에서 파일 삭제 중 오류 발생 시.
	 * @throws IllegalStateException 리스트의 경로 중 "estimateImages/"로 시작하지 않는 잘못된 형식의
	 *                               경로가 포함된 경우.
	 */
	public void deleteEstimateImages(List<String> imagesPath) {
		List<String> keys = imagesPath.stream().map(path -> {
			String key = path.replace(s3Url, "");
			if (!key.startsWith("estimateImages/")) {
				throw new IllegalStateException("* AWSS3 이미지 삭제 실패 : 데이터 형식 부적합.");
			}
			return key;
		}).toList();

		fileStorage.delete(keys);
	}

}
