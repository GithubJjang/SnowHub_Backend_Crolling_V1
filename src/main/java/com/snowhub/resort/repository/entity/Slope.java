package com.snowhub.resort.repository.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slope {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String resortName;
	private String slopeName;
	private String difficulty;
	private String weekly;
	private String nightTime;
	private String lateAtNight;
	private String etc;

	@Override
	public String toString() {
		return resortName+" "+slopeName+" "+difficulty+" "+weekly+" "+nightTime+" "+lateAtNight;
	}
}
