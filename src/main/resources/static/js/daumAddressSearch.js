export function searchAddress(callback) {
		new daum.Postcode({
			oncomplete : function(data) {

				var addr = ''; // 주소 변수
				var extraAddr = ''; // 참고항목 변수

				if (data.userSelectedType === 'R') { // 사용자가 도로명 주소를 선택했을 경우
					addr = data.roadAddress;
				} else { // 사용자가 지번 주소를 선택했을 경우(J)
					addr = data.jibunAddress;
				}

				// 사용자가 선택한 주소가 도로명 타입일때 참고항목을 조합한다.
				if (data.userSelectedType === 'R') {
					// 법정동명이 있을 경우 추가한다. (법정리는 제외)
					// 법정동의 경우 마지막 문자가 "동/로/가"로 끝난다.
					if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
						extraAddr += data.bname;
					}
					// 건물명이 있고, 공동주택일 경우 추가한다.
					if (data.buildingName !== '' && data.apartment === 'Y') {
						extraAddr += (extraAddr !== '' ? ', '
								+ data.buildingName : data.buildingName);
					}
					// 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
					if (extraAddr !== '') {
						extraAddr = ' (' + extraAddr + ')';
					}
					// 조합된 참고항목을 지번 주소 뒤에 붙인다.
					addr = addr + extraAddr;

				}
				
				if(callback){
					callback(data.zonecode,addr);
				}else{
					
				// 우편번호와 주소 정보를 해당 필드에 넣는다.
				document.getElementById('postcode').value=data.zonecode;
				document.getElementById('mainAddress').value=addr;
				
				// 커서를 상세주소 필드로 이동한다.
				document.getElementById("detailAddress").focus();
				}
				
			}
		}).open({popupKey: 'popup1'});
	}