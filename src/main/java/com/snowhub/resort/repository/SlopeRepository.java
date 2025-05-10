package com.snowhub.resort.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.snowhub.resort.repository.entity.Slope;

@Repository
public interface SlopeRepository extends JpaRepository<Slope,Integer> {

	@Query(value = "SELECT * FROM slope WHERE slopeName = :slopeName", nativeQuery = true)
	Slope findBySlopeName(@Param("slopeName") String slopeName);

	@Query(value = "SELECT * FROM slope WHERE resortName = :resortName", nativeQuery = true)
	List<Slope> findByResortName(@Param("resortName") String resortName);
}
