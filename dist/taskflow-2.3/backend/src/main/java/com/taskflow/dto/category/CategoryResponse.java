package com.taskflow.dto.category;

import com.taskflow.domain.Category;
import com.taskflow.domain.CategoryProperty;
import com.taskflow.domain.CategoryShare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 응답 DTO
 *
 * v2.0 변경사항:
 * - categoryCode, description, ownerUsername 필드 추가
 * - properties, shares 목록 추가
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private String categoryColor;
    private String ownerUsername;
    private String ownerName;
    private Integer sortOrder;
    private String useYn;
    private Integer propertyCount;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * 속성 목록
     */
    private List<PropertyInfo> properties;

    /**
     * 공유 목록
     */
    private List<ShareInfo> shares;

    /**
     * 엔티티를 응답 DTO로 변환 (기본)
     */
    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .categoryColor(category.getCategoryColor())
                .ownerUsername(category.getOwnerUsername())
                .ownerName(category.getOwnerName())
                .sortOrder(category.getSortOrder())
                .useYn(category.getUseYn())
                .propertyCount(category.getPropertyCount())
                .createdAt(category.getCreatedAt())
                .createdBy(category.getCreatedBy())
                .updatedAt(category.getUpdatedAt())
                .updatedBy(category.getUpdatedBy())
                .build();
    }

    /**
     * 엔티티를 응답 DTO로 변환 (속성/공유 포함)
     */
    public static CategoryResponse fromWithDetails(Category category) {
        if (category == null) {
            return null;
        }

        List<PropertyInfo> propertyInfos = null;
        if (category.getProperties() != null) {
            propertyInfos = category.getProperties().stream()
                    .map(PropertyInfo::from)
                    .collect(Collectors.toList());
        }

        List<ShareInfo> shareInfos = null;
        if (category.getShares() != null) {
            shareInfos = category.getShares().stream()
                    .map(ShareInfo::from)
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .categoryColor(category.getCategoryColor())
                .ownerUsername(category.getOwnerUsername())
                .ownerName(category.getOwnerName())
                .sortOrder(category.getSortOrder())
                .useYn(category.getUseYn())
                .propertyCount(category.getPropertyCount())
                .createdAt(category.getCreatedAt())
                .createdBy(category.getCreatedBy())
                .updatedAt(category.getUpdatedAt())
                .updatedBy(category.getUpdatedBy())
                .properties(propertyInfos)
                .shares(shareInfos)
                .build();
    }

    /**
     * 속성 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyInfo {
        private Long categoryPropertyId;  // TB_CATEGORY_PROPERTY.ID
        private Long categoryId;
        private Long propertyId;
        private String propertyName;
        private String propertyType;
        private String ownerType;  // GLOBAL, MANAGER, USER
        private String requiredYn;
        private Integer sortOrder;
        private String defaultValue;
        private LocalDateTime addedAt;

        public static PropertyInfo from(CategoryProperty cp) {
            if (cp == null) return null;
            return PropertyInfo.builder()
                    .categoryPropertyId(cp.getCategoryPropertyId())
                    .categoryId(cp.getCategoryId())
                    .propertyId(cp.getPropertyId())
                    .propertyName(cp.getPropertyName())
                    .propertyType(cp.getPropertyType())
                    .ownerType(cp.getOwnerType())
                    .requiredYn(cp.getRequiredYn())
                    .sortOrder(cp.getSortOrder())
                    .defaultValue(cp.getDefaultValue())
                    .addedAt(cp.getCreatedAt())
                    .build();
        }
    }

    /**
     * 공유 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareInfo {
        private Long shareId;
        private String shareType;
        private String shareTarget;
        private String shareTargetName;
        private String permission;
        private LocalDateTime sharedAt;
        private String sharedBy;

        public static ShareInfo from(CategoryShare cs) {
            if (cs == null) return null;
            return ShareInfo.builder()
                    .shareId(cs.getShareId())
                    .shareType(cs.getShareType())
                    .shareTarget(cs.getShareTarget())
                    .shareTargetName(cs.getShareTargetName())
                    .permission(cs.getPermission())
                    .sharedAt(cs.getCreatedAt())
                    .sharedBy(cs.getCreatedBy())
                    .build();
        }
    }
}
