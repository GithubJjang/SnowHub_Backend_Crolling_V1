package com.snowhub.resort.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.snowhub.resort.repository.SlopeRepository;
import com.snowhub.resort.repository.entity.Slope;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SlopeService {

	private final EntityManager entityManager;

	private final SlopeRepository slopeRepository;

	// 1. slope 저장하기
	@Transactional
	public void save(Slope slope){
		slopeRepository.save(slope);

	}
	// 2. slopeName으로 slope찾기
	public Slope findSlopeBySlopeName(String slopeName){
		// 못찾으면 null을 반환한다.
		Slope slope = slopeRepository.findBySlopeName(slopeName);

		return slope;
	}

	// 3. resortName으로 slope들 찾기
	public List<Slope> findSlopesByResortName(String resortName,String timeline){
		if(timeline.equals("day")){
			return slopeRepository.findByResortName(resortName);
		}
		else{
			// weekly, nightTime, lateAtNight
			String query = String.format("SELECT slopeName, difficulty, %s FROM slope WHERE resortName = :resortName", timeline);
			Query nativeQuery = entityManager.createNativeQuery(query);
			nativeQuery.setParameter("resortName", resortName);

			List<Object[]> slopeList = nativeQuery.getResultList();


			List<Slope> fetchedSlopeList =
			slopeList.stream().map(obj -> {
				String slopeName = (String)obj[0];
				String diffculty = (String)obj[1];
				String openStatus = (String)obj[2];

				String weekly = "-";
				String nightTime ="-";
				String lateAtNight ="-";

				if(timeline.equals("weekly")){
					weekly = openStatus;
				}
				else if(timeline.equals("nightTime")){
					nightTime = openStatus;
				}
				else if(timeline.equals("lateAtNight")){
					lateAtNight = openStatus;
				}
				else{
					throw new RuntimeException("There is no Time like " +openStatus);
				}


				Slope slope = Slope.builder()
					.resortName(resortName)
					.slopeName(slopeName)
					.difficulty(diffculty)
					.weekly(weekly)
					.nightTime(nightTime)
					.lateAtNight(lateAtNight)
					.build()
					;

				return slope;
			}).collect(Collectors.toCollection(ArrayList::new));

			for(Slope s:fetchedSlopeList){
				System.out.println(s.toString());
			}


			return fetchedSlopeList;

		}

	}

	// 3. slopeName으로 slope 찾은 다음 Update하기 ( 더티 체킹 )
	@Transactional
	public void updateSlopeBySlopeName( String slopeName ,Slope renewSlope ){
		Slope slope = slopeRepository.findBySlopeName(slopeName);
		slope.setWeekly(renewSlope.getWeekly());// 주간에 열렸는지 여부 업데이트
		slope.setNightTime(renewSlope.getNightTime());// 야간에 열렸는지 여부 업데이트
		slope.setLateAtNight(renewSlope.getLateAtNight());// 심야에 열렸는지 여부 업데이트
		slope.setDifficulty(renewSlope.getDifficulty());// 혹시나 난이도도 바뀔 수 있으므로, 업데이트
	}
}
