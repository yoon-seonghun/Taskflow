ysh , 26.01.20
MYSQL JDBC 특징이라는데... 이것때문에 한참 삽질함 
컬럼명 , 값 모두 대소문자 가림... 
그래서 아래 외부 인사 정보 테이블 참조용 View 테이블 생성용 샘플 남김 
무조건 컬럼명은 아래와 동일해야 함... 컬럼명은 무조건 소문자 사용 , 뷰 테이블명은 대문자 
로그인 아이디는 검사시 무조건 소문자로 변환해 조회 검사함 , Mapper 에서 where 절에 lower() 사용 

  -- VW_TASKFLOW_DEPARTMENT 수정
  create or replace view VW_TASKFLOW_DEPARTMENT as
  select
    id as department_id,
    uid as department_code,
    name as department_name,
    upper_id as parent_code,
    seq as sort_order,
    case when del=0 then 'Y' else 'N' end as use_yn,
    created_date as created_at,
    1 as created_by,
    modified_date as updated_at,
    null as updated_by
  from tb_department a
  where a.del=0;

  -- VW_TASKFLOW_USER 수정
  create or replace view VW_TASKFLOW_USER as
  select
    id as user_id,
    uid as username,
    pw as password,
    full_name as name,
    e_mail as email,
    (select uid from tb_department b where b.uid = a.department_id) as department_code,
    (select name from tb_department b where b.uid = a.department_id) as department_name,
    case when del = 0 then 'Y' else 'N' end as use_yn,
    (select uid from tb_rank b where b.uid = a.rank_id) as position_code,
    (select name from tb_rank b where b.uid = a.rank_id) as position_name,
    (select seq from tb_rank b where b.uid = a.rank_id) as position_sort_order,
    upper(role) as role,
    case when head = 1 then 'Y' else 'N' end as head_yn,
    null as last_login_at,
    created_date as created_at,
    'admin' as created_by,
    modified_date as updated_at,
    null as updated_by
  from tb_staff a
  where uid in (
    select `tb_staff`.`uid`
    from `tb_staff`
    where `tb_staff`.`sync_system` like '%sv001%'
    group by `tb_staff`.`uid`)
  order by department_code, position_code;
  
  -- VW_TASKFLOW_POSITION 수정
  create or replace view VW_TASKFLOW_POSITION as
select id as position_id , uid as position_code , name as position_name,
   coalesce(seq,999) as sort_order,case when del = 0 then 'Y' else 'N' end as use_yn,
   created_date as created_at , 'admin' as created_by,
    modified_date as updated_at,null as updated_by
from tb_rank;