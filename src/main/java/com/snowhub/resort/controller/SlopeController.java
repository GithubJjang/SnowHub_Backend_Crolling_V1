package com.snowhub.resort.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snowhub.resort.repository.entity.Slope;
import com.snowhub.resort.service.SlopeService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class SlopeController {

	private final SlopeService slopeService;
	@GetMapping("/slope")
	public ResponseEntity<Object> sayHello(
		@RequestParam(name = "resort")String resort,
		@RequestParam(name = "timeline")String timeline) {

		if(resort.equals("phoenix")){
			resort="PhyungChang";
		}


		List<Slope> slopeList = slopeService.findSlopesByResortName(resort,timeline);

		Slope header = Slope.builder()
				.slopeName("슬로프이름")
				.resortName("리조트이름")
				.difficulty("난이도")
				.weekly("주간")
				.nightTime("야간")
				.lateAtNight("심야")
				.build()
			;

		try{
			slopeList.add(0,header);
		}
		catch (Exception e){
			e.printStackTrace();
		}


		return ResponseEntity.ok(slopeList);
	}
}
