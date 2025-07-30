package net.chamman.moonnight.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HttpStatusCode {
	
	// Success
	SUCCESS(200,"2000","message.success"),
	SUCCESS_NO_DATA(204,"2040","message.success.no_data"),
	READ_SUCCESS(200,"2001","message.success.read"),
	READ_SUCCESS_NO_DATA(204,"2041","message.success.read_no_data"),
	CREATE_SUCCESS(201,"2010","message.success.create"),
	UPDATE_SUCCESS(200,"2002","message.success.update"),
	DELETE_SUCCESS(200,"2003","message.success.delete"),
	
	// Common
	ILLEGAL_REQUEST(400, "4000", "message.common.illegal_request"), // 요청 이상
	ILLEGAL_INPUT_VALUE(400, "4001", "message.common.illegal_input_value"), // 입력 받은 값 이상
	REQUEST_BODY_NOT_VALID(400, "4002", "message.common.request_body_not_valid"), // body 이상
	AUTHORIZATION_FAILED(401,"4030","message.common.authorization_failed"), // 권한 이상
	METHOD_NOT_ALLOWED(405, "4050", "message.common.method_not_allowed"), // 요청 url 이상
	TOO_MANY_REQUEST(429,"4290","message.common.too_many_request"), 
	INTERNAL_SERVER_ERROR(500, "5000", "message.common.internal_server_error"),
	
	// User 453
	USER_NOT_FOUND(404,"4530","message.user.not_found"),
	USER_PASSWORD_MISMATCH(404,"4530","message.user.password_mismatch"),
	USER_ONLT_LOCAL(401,"4010","message.user.only_local"),
	
	EMAIL_NOT_EXISTS(200,"2070","message.user.email_not_exists"),
	PHONE_NOT_EXISTS(200,"2071","message.user.phone_not_exists"),
	EMAIL_ALREADY_EXISTS(409,"4531","message.user.email_already_exists"),
	PHONE_ALREADY_EXISTS(409,"4532","message.user.phone_already_exists"),
	DELETED_EMAIL_EXISTS(200,"2072","message.user.deleted_email_exists"),
	
	USER_STATUS_STAY(403,"4533","message.user.status_stay"),
	USER_STATUS_STOP(401,"4534","message.user.status_stop"),
	USER_STATUS_DELETE(410,"4535","message.user.status_delete"),
	
	OAUTH_NOT_FOUND(404,"4530","message.user.not_found"),
	OAUTH_STATUS_STAY(403,"4533","message.user.status_stay"),
	OAUTH_STATUS_STOP(401,"4534","message.user.status_stop"),
	
	// Address 454
	ADDRESSS_NOT_FOUND(404,"4540","message.address.not_found"),
	ADDRESS_INVALID_VALUE(400, "4541", "message.address.address_invalid_value"), // Daum 조회 결과 없는 주소

	// Comment 455
	COMMENT_NOT_FOUND(404,"4550","message.comment.not_found"),
	COMMENT_STATUS_DELETE(400,"4551","message.comment.status_delete"),
	
	// Estimate 456
	ESTIMATE_NOT_FOUND(404,"4560","message.estimate.not_found"),
	ESTIMATE_STATUS_DELETE(404,"4561","message.estimate.status_delete"),
	
	// Sign 452
	SIGNIN_FAILED(404,"4520","message.sign.signin_failed"),
	SIGNIN_FAILED_OUT(404,"4521","message.sign.signin_failed_out"),
	SIGNUP_EXIST_LOCAL(302,"4522","message.sign.signup_exist_local"),
	SIGNUP_EXIST_OAUTH(302,"4523","message.sign.signup_exist_oauth"),
//	AUTHENTICATION_FAILED(401,"4523","message.sign.authentication_failed"),
	
	// JWT 457
	JWT_ILLEGAL(400,"4570","meesage.jwt.illegal"), // 토큰 값 부적합 또는 내부 claims 부적합
	JWT_EXPIRED(403,"4571","message.jwt.expired"), // 토큰 만료
	JWT_VALIDATE_FIAL(401,"4572","message.jwt.validate_fail"), // 토큰 검증 실패
	JWT_CREATE_FIAL(500,"5001","message.jwt.create_fail"), // 토큰 생성 실패
	
	// Token 458
	TOKEN_ILLEGAL(400,"4580","message.token.illegal"), // 토큰 값 null 또는 없음
	TOKEN_NOT_FOUND(404,"4581","meesage.token.not_found"), // 토큰 없음
	TOKEN_EXPIRED(403,"4582","message.token.expired"), // 토큰 만료
	TOKEN_VALUE_MISMATCH(401,"4583","message.token.illegal"), // 토큰 값 불일치
	TOKEN_GET_FIAL(500,"5002","message.common.internal_server_error"), // Redis 토큰 조회 실패
	TOKEN_SET_FIAL(500,"5003","message.common.internal_server_error"), // Redis 토큰 저장 실패

	// Verification 459
	VERIFICATION_NOT_FOUND(404,"4590","message.verification.not_found"),
	MISMATCH_VERIFICATION_CODE(401,"4591","message.verification.mismatch_code"),
	MISMATCH_RECIPIENT(401,"4592","message.verification.mismatch_recipient"),
	VERIFICATION_EXPIRED(403,"4593","message.verification.expired"),
	NOT_VERIFY(403,"4594","message.verification.not_verify"),
	TOO_MANY_VERIFY(429,"4595","message.verification.too_many_verify"),
	
	// Crypto 460
	ENCRYPT_FAIL(500,"5004","message.common.internal_server_error"),
	DECRYPT_FAIL(500,"5005","message.common.internal_server_error"),
	
	// S3 461
	S3_UPLOAD_FAIL(500,"4610","message.s3.upload_fail"),
	S3_DELETE_FAIL(500,"4611","message.s3.delete_fail"),
	
	// Email 462
	EMAIL_SEND_FAIL(500,"5006","message.mail.send_fail"),
	
	// Sms 463
	SMS_SEND_FAIL(500,"5007","message.sms.send_fail"),
	
	// Map Road Search
	ROAD_SEARCH_FAIL(500,"5008","message.map.road_search_fail"),
	
	// Transaction 464
	ENCODING_FAIL(400, "4000", "message.common.illegal_input_value"),
	DECODING_FAIL(400, "4000", "message.common.illegal_input_value")
	
	; 
	
	
	
	
	// 필요한 만큼 쭉쭉 추가하면 됨!
	
	private final int status;
	private final String code;
	private final String messageKey;

}
