package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;

import java.util.List;

public interface MemberCustomRepository {

    List<MemberTeamDTO> search(MemberSearchCondition condition);
    Page<MemberTeamDTO> searchWithPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDTO> searchWithPageComplex(MemberSearchCondition condition, Pageable pageable);

}
