package com.taskflow.mapper;

import com.taskflow.domain.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 시스템 설정 Mapper
 */
@Mapper
public interface SystemConfigMapper {

    /**
     * 설정 그룹별 목록 조회
     */
    List<SystemConfig> selectByGroup(@Param("configGroup") String configGroup);

    /**
     * 특정 설정 조회
     */
    SystemConfig selectByGroupAndKey(
            @Param("configGroup") String configGroup,
            @Param("configKey") String configKey
    );

    /**
     * 전체 설정 조회
     */
    List<SystemConfig> selectAll();

    /**
     * 설정 값 업데이트 (일반 값)
     */
    int updateValue(
            @Param("configGroup") String configGroup,
            @Param("configKey") String configKey,
            @Param("configValue") String configValue,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 설정 값 업데이트 (암호화된 값)
     */
    int updateEncryptedValue(
            @Param("configGroup") String configGroup,
            @Param("configKey") String configKey,
            @Param("configValueEncrypted") String configValueEncrypted,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 설정 추가
     */
    int insert(SystemConfig config);

    /**
     * 설정 삭제
     */
    int deleteByGroupAndKey(
            @Param("configGroup") String configGroup,
            @Param("configKey") String configKey
    );
}
