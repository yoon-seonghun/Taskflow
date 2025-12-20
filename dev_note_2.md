● 현재 뷰타입 관리 방식

  현재 구현 (localStorage)

  // stores/board.ts

  // 뷰타입 저장
  function setViewType(type: ViewType) {
    viewType.value = type
    localStorage.setItem('taskflow_viewType', type)  // 브라우저에 저장
  }

  // 뷰타입 로드
  function loadViewType() {
    const saved = localStorage.getItem('taskflow_viewType')
    if (saved) viewType.value = saved
  }

  비교

  | 항목      | 현재 (localStorage)   | DB (DEFAULT_VIEW) |
  |-----------|-----------------------|-------------------|
  | 저장 위치 | 브라우저              | 서버 DB           |
  | 적용 범위 | 전역 (모든 보드 동일) | 보드별 개별 설정  |
  | 다른 기기 | 공유 안됨             | 공유됨            |
  | 용도      | 사용자 개인 선호      | 보드 기본값       |

  결론

  DB의 DEFAULT_VIEW는 현재 사용되지 않고 있습니다.

  - 조회 쿼리에는 포함되어 있지만
  - 실제 뷰타입 표시는 localStorage 값을 사용

  설계 의도 추측

  원래 설계는 "보드별 기본 뷰"를 지원하려 했으나, 현재는 "사용자 전역 선호 뷰"로 단순화된 것으로 보입니다. DEFAULT_VIEW 컬럼은 향후 보드별 기본 뷰 기능 구현 시 사용될 수      
  있습니다.

  ● DEFAULT_VIEW 컬럼 위치

  1. DB 스키마 정의

  파일: docker/mysql/init/01_schema.sql
  테이블: TB_BOARD

  CREATE TABLE TB_BOARD (
      BOARD_ID BIGINT NOT NULL AUTO_INCREMENT,
      BOARD_NAME VARCHAR(200) NOT NULL,
      DESCRIPTION VARCHAR(500) NULL,
      OWNER_ID BIGINT NOT NULL,
      DEFAULT_VIEW VARCHAR(20) NULL DEFAULT 'TABLE',  -- ← 여기
      COLOR VARCHAR(7) NULL,
      ...
  );

  2. MyBatis 매퍼

  파일: backend/src/main/resources/mapper/BoardMapper.xml

  <!-- ResultMap -->
  <result property="defaultView" column="DEFAULT_VIEW"/>

  <!-- SELECT 쿼리 -->
  SELECT b.DEFAULT_VIEW, ...

  <!-- INSERT 쿼리 -->
  INSERT INTO TB_BOARD (..., DEFAULT_VIEW, ...)

  <!-- UPDATE 쿼리 -->
  DEFAULT_VIEW = #{defaultView}

  3. 용도

  | 값     | 설명               |
  |--------|--------------------|
  | TABLE  | 테이블 뷰 (기본값) |
  | KANBAN | 칸반 뷰            |
  | LIST   | 리스트 뷰          |

  보드를 열 때 어떤 뷰 타입으로 표시할지 저장하는 컬럼입니다.