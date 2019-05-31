package org.icgc.argo.program_service.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.Set;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends PagingAndSortingRepository<T, ID>, JpaSpecificationExecutor {

  Set<T> findAllByIdIn(Collection<ID> ids);

}
