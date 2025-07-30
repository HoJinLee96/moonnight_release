package net.chamman.moonnight.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("이호진 moonnight API 문서")
						.version("v1")
						.description("이 프로젝트의 REST API 명세입니다.")
						)
				.components(commonComponents());
	}
	
	private Components commonComponents() {
		return new Components()
				// === 공통 파라미터 정의 ===
				.addParameters("ClientType", CommonParameter.clientType())
				.addParameters("Phone", CommonParameter.phone())
				// === 공통 응답 정의 ===
				.addResponses("SuccessEmailResponse", CommonResponse.successEmailResponse())
				.addResponses("BadRequestTokenMissing", CommonResponse.badRequestTokenMissing())
				// === 공통 스키마 정의 ===
				.addSchemas("ApiResponseString", CommonSchema.apiResponseString())
				.addSchemas("ApiResponseNull", CommonSchema.apiResponseNull())
				// === 공통 보안 스키마 정의 ===
				.addSecuritySchemes("SignInAccessJwt", CommonSecurityScheme.signInAccessJwt())
				.addSecuritySchemes("SignInRefreshJwt", CommonSecurityScheme.signInRefreshJwt())
				.addSecuritySchemes("VerificationPhoneToken", CommonSecurityScheme.verificationPhoneToken())
				.addSecuritySchemes("VerificationEmailToken", CommonSecurityScheme.verificationEmailToken())
				.addSecuritySchemes("AccessFindpwToken", CommonSecurityScheme.accessFindpwToken())
				.addSecuritySchemes("AccessPasswordToken", CommonSecurityScheme.accessPasswordToken())
				;
	}
	
	static class CommonParameter {
		
		public static Parameter clientType() {
			return new Parameter()
					.name("X-Client-Type")
					.in("header")
					.required(false)
					.description("web or mobile 값에 따라 요청, 응답 방식이 달라짐.")
					.schema(new Schema<String>().type("string").example("web or mobile"));
		}
		
		public static Parameter phone() {
			return new Parameter()
					.name("phone")
					.in("form-data")
					.required(true)
					.description("휴대폰 번호")
					.schema(new Schema<String>().type("string").example("010-1234-5678"));
		}
		
		public static Parameter email() {
			return new Parameter()
					.name("email")
					.in("form-data")
					.required(true)
					.description("이메일")
					.schema(new Schema<String>().type("string").example("uesr@example.com"));
		}
	}
	
	static class CommonResponse {
		public static ApiResponse successEmailResponse() {
			return new ApiResponse()
					.description("조회 성공 - 유저 존재 시 이메일 반환, 없으면 data는 null")
					.content(new Content().addMediaType("application/json",
							new MediaType()
							.schema(new Schema<>().$ref("#/components/schemas/ApiResponseString"))
							.addExamples("유저 존재", new Example().value("{\"statusCode\": 200, \"message\": \"조회 성공\", \"data\": \"user@example.com\"}"))
							.addExamples("유저 없음", new Example().value("{\"statusCode\": 200, \"message\": \"조회 성공\", \"data\": null}"))
							));
		}
		
		public static ApiResponse badRequestTokenMissing() {
			return new ApiResponse()
					.description("토큰 누락 또는 유효하지 않음")
					.content(new Content().addMediaType("application/json",
							new MediaType()
							.schema(new Schema<>().$ref("#/components/schemas/ApiResponseNull"))
							.addExamples("토큰 누락", new Example().value("{\"statusCode\": 400, \"message\": \"토큰 누락\", \"data\": null}"))
							));
		}
	}
	
	static class CommonSchema {
		public static Schema<?> apiResponseString() { // Schema<Object> 대신 Schema<?> 사용 가능
			return new Schema<Object>()
					.type("object")
					.addProperty("statusCode", new Schema<Integer>().type("integer").example(200))
					.addProperty("message", new Schema<String>().type("string").example("조회 성공"))
					.addProperty("data", new Schema<String>().type("string").nullable(true).example("user@example.com"));
		}
		
		public static Schema<?> apiResponseNull() {
			return new Schema<Object>()
					.type("object")
					.addProperty("statusCode", new Schema<Integer>().type("integer").example(400))
					.addProperty("message", new Schema<String>().type("string").example("에러 메시지"))
					.addProperty("data", new Schema<>().type("object").nullable(true).example("null")); // example을 문자열 "null"로 표현
		}
	}
	
	static class CommonSecurityScheme{
		public static SecurityScheme signInAccessJwt() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Access-Token")
					.description("Access Token. (웹: 쿠키, 모바일: 헤더)");
		}
		
		public static SecurityScheme signInRefreshJwt() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Refresh-Token")
					.description("Refresh Token. (웹: 쿠키, 모바일: 헤더)");
		}
		
		public static SecurityScheme verificationPhoneToken() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.APIKEY)
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Verification-Phone-Token")
					.description("휴대폰 인증 성공 후 발급된 토큰. (웹: 쿠키, 모바일: 헤더)");
		}
		
		public static SecurityScheme verificationEmailToken() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Verification-Email-Token")
					.description("이메일 인증 성공 후 발급된 토큰 (웹: 쿠키, 모바일: 헤더)");
		}
		
		public static SecurityScheme accessFindpwToken() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Access-Findpw-Token")
					.description("비밀번호 찾기위해 인증 성공 후 발급된 토큰 (웹: 쿠키, 모바일: 헤더)");
		}
		
		public static SecurityScheme accessPasswordToken() {
			return new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("X-Access-Password-Token")
					.description("비밀번호 확인 후 발급된 토큰 (웹: 쿠키, 모바일: 헤더)");
		}
		
	}

}
