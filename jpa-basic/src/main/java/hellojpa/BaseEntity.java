package hellojpa;

import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    private String createBy;

    private LocalDateTime createdDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

}
