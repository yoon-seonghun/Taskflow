package com.taskflow.dto.user;

import com.taskflow.common.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 검색 요청 DTO
 */
@Getter
@Setter
public class UserSearchRequest extends PageRequest {

    /**
     * 검색 키워드 (이름, 아이디)
     */
    private String keyword;

    /**
     * 부서 ID 필터
     */
    private Long departmentId;

    /**
     * 사용 여부 필터 (Y/N)
     */
    private String useYn;
}
