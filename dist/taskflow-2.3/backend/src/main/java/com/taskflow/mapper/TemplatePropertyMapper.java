package com.taskflow.mapper;

import com.taskflow.domain.TemplateProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 템플릿 속성 Mapper 인터페이스
 */
@Mapper
public interface TemplatePropertyMapper {

    /**
     * 템플릿별 속성 목록 조회
     */
    List<TemplateProperty> findByTemplateId(Long templateId);

    /**
     * 템플릿 속성 등록
     */
    void insert(TemplateProperty property);

    /**
     * 템플릿 속성 일괄 등록
     */
    void insertBatch(@Param("properties") List<TemplateProperty> properties);

    /**
     * 템플릿 속성 논리 삭제
     */
    void softDeleteByTemplateId(@Param("templateId") Long templateId,
                                 @Param("deletedBy") String deletedBy);

    /**
     * 템플릿 속성 물리 삭제
     */
    void deleteByTemplateId(Long templateId);
}
