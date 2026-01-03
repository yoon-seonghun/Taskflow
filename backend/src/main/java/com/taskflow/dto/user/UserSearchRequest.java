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
     * 부서 코드 필터
     */
    private String departmentCode;

    /**
     * departmentCode getter (Lombok 호환성)
     */
    public String getDepartmentCode() {
        return departmentCode;
    }

    /**
     * departmentCode setter (Lombok 호환성)
     */
    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    /**
     * 사용 여부 필터 (Y/N)
     */
    private String useYn;

    /**
     * keyword getter (Lombok 호환성)
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * keyword setter (Lombok 호환성)
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * useYn getter (Lombok 호환성)
     */
    public String getUseYn() {
        return useYn;
    }

    /**
     * useYn setter (Lombok 호환성)
     */
    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }
}
