package net.chamman.moonnight.domain.address;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESSS_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.address.dto.AddressRequestDto;
import net.chamman.moonnight.domain.address.dto.AddressResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.infra.RoadSearchException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.map.impl.DaumMapClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {
	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final DaumMapClient daumMapClient;
	private final Obfuscator obfuscator;
	
	/** 주소 등록
	 * @param userId
	 * @param addressRequestDto
	 * 
	 * @throws IllegalAddressValueException {@link DaumMapClient#validateAddress} 일치하는 주소가 없음
	 * @throws RoadSearchException {@link DaumMapClient#validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return
	 */
	@Transactional
	public AddressResponseDto registerAddress(int userId, AddressRequestDto addressRequestDto) {
		
		daumMapClient.validateAddress(addressRequestDto.postcode(), addressRequestDto.mainAddress());
		
		User user = userRepository.getReferenceById(userId);
		
		Address address = addressRequestDto.toEntity(user);
		addressRepository.save(address);
		
		return AddressResponseDto.fromEntity(address, obfuscator);
	}
	
	/** 단일 주소 조회
	 * @param userId
	 * @param encodedAddressId
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return 주소
	 */
	public AddressResponseDto getAddress(int userId, int encodedAddressId) {
		
		Address address = getAuthorizedAddress(userId, encodedAddressId);
		
		return AddressResponseDto.fromEntity(address, obfuscator);
	}
	
	/** 주소 리스트 조회
	 * @param userId
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return 주소 리스트
	 */
	public List<AddressResponseDto> getAddressList(int userId) {
		log.debug("* UserId: [{}] 주소 목록 조회.", LogMaskingUtil.maskId(userId, MaskLevel.MEDIUM));
		List<Address> list = addressRepository.findByUserOrderByPrimaryAndDate(userId);
		
		return list.stream()
				.map(e->AddressResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** 주소 수정
	 * @param userId
	 * @param encodedAddressId
	 * @param addressRequestDto
	 * 
	 * @throws IllegalAddressValueException {@link DaumMapClient#validateAddress} 일치하는 주소가 없음
	 * @throws RoadSearchException {@link DaumMapClient#validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return
	 */
	@Transactional
	public void updateAddress(int userId, int encodedAddressId, AddressRequestDto addressRequestDto) {
		
		daumMapClient.validateAddress(addressRequestDto.postcode(), addressRequestDto.mainAddress());
		
		Address address = getAuthorizedAddress(userId, encodedAddressId);
		
		address.update(addressRequestDto);
	}
	
	/** 대표 주소 선정
	 * @param userId
	 * @param encodedAddressId
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 */
	@Transactional
	public void updatePrimary(int userId, int encodedAddressId) {
		
		Address address = getAuthorizedAddress(userId, encodedAddressId);
		
		addressRepository.unsetPrimaryForUser(userId);
		
		address.setPrimary(true);
	}
	
	/** 주소 삭제
	 * @param userId
	 * @param encodedAddressId
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 */
	@Transactional
	public void deleteAddress(int userId, int encodedAddressId) {
		
		Address address = getAuthorizedAddress(userId, encodedAddressId);
		
		addressRepository.delete(address);
	}
	
	/** 주소 GET 및 유저 검증
	 * @param userId
	 * @param encodedAddressId
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return Address
	 */
	private Address getAuthorizedAddress(int userId, int encodedAddressId){
		
		Address address = addressRepository.findById(obfuscator.decode(encodedAddressId))
				.orElseThrow(() -> new NoSuchDataException(ADDRESSS_NOT_FOUND,"일치하는 데이터 없음. encodedAddressId: " + encodedAddressId));
		
		if(address.getUser().getUserId()!=userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"주소 조회 권한 이상. address.getUser().getUserId(): "+address.getUser().getUserId()+"!= userId: "+userId);
		}
		
		return address;
	}
}