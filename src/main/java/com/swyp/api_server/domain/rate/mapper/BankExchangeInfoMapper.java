package com.swyp.api_server.domain.rate.mapper;

import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;
import com.swyp.api_server.entity.BankExchangeInfo;
import org.mapstruct.*;

import java.util.List;

/**
 * BankExchangeInfo Entity와 DTO 간 매핑을 담당하는 MapStruct 매퍼
 * - 컴파일 타임에 자동으로 구현체 생성
 * - 수동 변환 코드 제거로 코드 중복 감소
 * - 타입 안정성 보장
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BankExchangeInfoMapper {
    
    /**
     * Entity → ResponseDTO 변환
     * @param entity BankExchangeInfo 엔티티
     * @return BankExchangeInfoResponseDTO
     */
    BankExchangeInfoResponseDTO toResponseDTO(BankExchangeInfo entity);
    
    /**
     * Entity 리스트 → ResponseDTO 리스트 변환
     * @param entities BankExchangeInfo 엔티티 리스트
     * @return BankExchangeInfoResponseDTO 리스트
     */
    List<BankExchangeInfoResponseDTO> toResponseDTOs(List<BankExchangeInfo> entities);
    
    /**
     * ResponseDTO → Entity 변환 (캐시된 DTO를 Entity로 변환할 때 사용)
     * @param responseDTO BankExchangeInfoResponseDTO
     * @return BankExchangeInfo 엔티티
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BankExchangeInfo toEntity(BankExchangeInfoResponseDTO responseDTO);
    
    /**
     * ResponseDTO 리스트 → Entity 리스트 변환
     * @param responseDTOs BankExchangeInfoResponseDTO 리스트
     * @return BankExchangeInfo 엔티티 리스트
     */
    List<BankExchangeInfo> toEntities(List<BankExchangeInfoResponseDTO> responseDTOs);
    
    /**
     * RequestDTO → Entity 변환 (새로운 엔티티 생성 시)
     * @param requestDTO BankExchangeInfoRequestDTO
     * @return BankExchangeInfo 엔티티
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    BankExchangeInfo toEntity(BankExchangeInfoRequestDTO requestDTO);
    
    /**
     * RequestDTO로 기존 Entity 업데이트 (수정 시) - 관리자용
     * @param requestDTO 요청 DTO
     * @param target 업데이트할 기존 엔티티
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true) // 활성 상태는 별도 메서드로 관리
    void updateEntityFromRequestDTO(BankExchangeInfoRequestDTO requestDTO, @MappingTarget BankExchangeInfo target);
    
    /**
     * 부분 업데이트 (null 값은 무시) - 관리자용
     * @param requestDTO 요청 DTO
     * @param target 업데이트할 기존 엔티티
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void partialUpdateEntityFromRequestDTO(BankExchangeInfoRequestDTO requestDTO, @MappingTarget BankExchangeInfo target);
}