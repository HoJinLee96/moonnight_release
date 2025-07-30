<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%-- <%@ page import="dto.request.UserCreateRequestDto" %> --%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>소셜 계정 확인</title>
<style type="text/css">
#mainContainer{
	max-width: 1200px;
	margin: 0 auto;
	padding-top: 30px;
	min-height: 1080px;
}
#confirmTable{
border: 1px solid #dadada;
padding: 20px 50px;
margin: 30px auto;
text-align: center;
font-size: 14px;
}
</style>
</head>
<body>
<%@ include file = "/WEB-INF/view/main/main_header.jsp" %>
<%-- <%
UserCreateRequestDto confirmUserDto = (UserCreateRequestDto) session.getAttribute("confirmUserDto");
String email = confirmUserDto.getEmail();
String[] emailParts = email.split("@");
String idPart = emailParts[0]; // 이메일의 아이디 부분
String domainPart = emailParts[1]; // 이메일의 도메인 부분

// 아이디의 앞 두 자리를 제외한 나머지 부분을 *로 변환
String encryptedIdPart = idPart.substring(0, 2) + "*".repeat(idPart.length() - 2);

// 최종 암호화된 이메일
String encryptedEmail = encryptedIdPart + "@" + domainPart;

String createAt = String.valueOf(confirmUserDto.getCreatedAt());
String[] createAtParts = createAt.split("T");
String dayPart = createAtParts[0];
%> --%>
	<div id ="mainContainer">
	
	<table id = "confirmTable">
	<tbody>
	<tr><td>같은 이메일로 가입된 계정이 있습니다.</td></tr>
	<tr><td>&nbsp;</td></tr> <!-- 빈 줄 -->
	<tr><td><span id="email"><%-- <%= encryptedEmail %> --%></span></td></tr>
	<tr><td><span id="email">가입 날짜 :  <%-- <%= dayPart %> --%></span></td></tr>
	<tr><td>&nbsp;</td></tr> <!-- 빈 줄 -->
	<c:choose>
	<c:when test="${sessionScope.confirmOAuthDto.provider =='NAVER'}">
	<tr><td><button id="linkExistingUserButton" >네이버와 기존 계정 연동하기</button></td></tr>
	<tr><td><button id="createNewNaverOauthButton" >네이버로 신규 계정 가입하기</button></td></tr>
	    <script>
        window.addEventListener('message', function(event) {
            if (event.data.loginStatus === 200) {
            	naverRegisterConnect();
            }
        });
        document.getElementById("createNewNaverOauthButton").addEventListener("click", function(){
        	naverRegister();
          });
    </script>
	</c:when>
	<c:when test="${sessionScope.confirmOAuthDto.provider == 'KAKAO'}">
	<tr><td><button id="linkExistingUserButton" >카카오와 기존 계정 연동하기</button></td></tr>
	<tr><td><button id="createNewKakaoOauthButton" >카카오로 신규 계정 가입하기</button></td></tr>
		    <script>
        window.addEventListener('message', function(event) {
            if (event.data.loginStatus === 200) {
            	kakaoRegisterConnect();
            }
        });
        document.getElementById("createNewKakaoOauthButton").addEventListener("click", function(){
        	kakaoRegister();
          });
    </script>
	</c:when>
	</c:choose>
	</tbody>
	</table>
	
	</div>

<%@ include file = "/WEB-INF/view/main/main_footer.jsp" %>
</body>
<script type="text/javascript">
document.getElementById("linkExistingUserButton").addEventListener("click",function(){
	openFindWindow("/loginBlank")
});

function openFindWindow(url) {
    // 새 창의 크기 지정
    const width = 500;
    const height = 600;

    // 창의 중앙 위치 계산
    const left = window.screenX + (window.outerWidth / 2) - (width / 2);
    const top = window.screenY + (window.outerHeight / 2) - (height / 2);

    // 새로운 창 열기
    window.open(
        url,  // 열고자 하는 URL
        '_blank',  // 새 창의 이름 또는 _blank (새 탭)
        'width=' + width + ',height=' + height + ',top=' + top + ',left=' + left  // 창의 크기와 위치
    );
    return false; // 기본 링크 동작을 막기 위해 false를 반환
}

function naverRegisterConnect(){
        event.preventDefault();
        
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '/naver/register/connect', true); // 비동기식 요청
        
        xhr.onload = function() {
            if (xhr.status === 200) {
            	naverTokenRefresh();
            } else if (xhr.status === 404) {
                alert("재로그인 후 다시 시도해주세요.");
                location.href = "/logout";
            } else if (xhr.status === 500) {
                alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
                location.href = "/logout";
            } else {
                alert("알 수 없는 오류가 발생했습니다.");
                location.href = "/logout";
            }
        };
        xhr.send();
    }
    
	function naverRegister(){
	    event.preventDefault();
	    
	    var xhr = new XMLHttpRequest();
	    xhr.open('POST', '/naver/register', true); // 비동기식 요청
	    
	    xhr.onload = function() {
	        if (xhr.status === 200) {
	        	naverTokenRefresh();
	        } else if (xhr.status === 404) {
	            alert("재로그인 후 다시 시도해주세요.");
	            location.href = "/logout";
	        } else if (xhr.status === 500) {
	            alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
	            location.href = "/logout";
	        } else {
	            alert("알 수 없는 오류가 발생했습니다.");
	            location.href = "/logout";
	        }
	    };
	    xhr.send();
	}
    
function naverTokenRefresh(){
    event.preventDefault();
    
    var xhr = new XMLHttpRequest();
    xhr.open('GET', '/naver/token/refresh', true); // 비동기식 요청
    
    xhr.onload = function() {
        if (xhr.status === 200) {
            location.href = "/home";
        } else if (xhr.status === 404) {
            alert("재로그인 후 다시 시도해주세요.");
            location.href = "/logout";
        } else if (xhr.status === 500) {
            alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
            location.href = "/logout";
        } else {
            alert("알 수 없는 오류가 발생했습니다.");
            location.href = "/logout";
        }
    };
    xhr.send();
}
function kakaoRegisterConnect(){
    event.preventDefault();
    
    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/kakao/register/connect', true); // 비동기식 요청
    
    xhr.onload = function() {
        if (xhr.status === 200) {
        	kakaoTokenRefresh();
        } else if (xhr.status === 404) {
            alert("재로그인 후 다시 시도해주세요.");
            location.href = "/logout";
        } else if (xhr.status === 500) {
            alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
            location.href = "/logout";
        } else {
            alert("알 수 없는 오류가 발생했습니다.");
            location.href = "/logout";
        }
    };
    xhr.send();
}

function kakaoRegister(){
    event.preventDefault();
    
    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/kakao/register', true); // 비동기식 요청
    
    xhr.onload = function() {
        if (xhr.status === 200) {
        	kakaoTokenRefresh();
        } else if (xhr.status === 404) {
            alert("재로그인 후 다시 시도해주세요.");
            location.href = "/logout";
        } else if (xhr.status === 500) {
            alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
            location.href = "/logout";
        } else {
            alert("알 수 없는 오류가 발생했습니다.");
            location.href = "/logout";
        }
    };
    xhr.send();
}

function kakaoTokenRefresh(){
	event.preventDefault();

	var xhr = new XMLHttpRequest();
	xhr.open('GET', '/kakao/token/refresh', true); // 비동기식 요청
	
	xhr.onload = function() {
	    if (xhr.status === 200) {
	        location.href = "/home";
	    } else if (xhr.status === 404) {
	        alert("재로그인 후 다시 시도해주세요.");
	        location.href = "/logout";
	    } else if (xhr.status === 500) {
	        alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
	        location.href = "/logout";
	    } else {
	        alert("알 수 없는 오류가 발생했습니다.");
	        location.href = "/logout";
	    }
	};
	xhr.send();
}
</script>
</html>