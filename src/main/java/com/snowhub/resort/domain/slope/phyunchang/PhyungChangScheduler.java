package com.snowhub.resort.domain.slope.phyunchang;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.snowhub.resort.repository.entity.Slope;
import com.snowhub.resort.service.SlopeService;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;


@Component
public class PhyungChangScheduler {

	@Autowired
	private SlopeService slopeService;

	@Autowired
	@Qualifier("Master_redisTemplate")
	private RedisTemplate<String,String> redisTemplate;

	@Scheduled(fixedDelay = 600000)
	public void crollPhyunChangSlope(){
		WebDriverManager.chromedriver().setup();

		// 랜덤 User-Agent 리스트
		String[] userAgents = {
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
		};

		// 랜덤 User-Agent 선택
		Random rand = new Random();
		String userAgent = userAgents[rand.nextInt(userAgents.length)];

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new"); // headless 모드
		options.addArguments("user-agent=" + userAgent); // 랜덤 User-Agent 설정

		// 추가적인 헤더 설정 (필요시)
		options.addArguments("--disable-blink-features=AutomationControlled"); // 이거 false 나와야 자동화 회피 가능하다
		options.addArguments("--disable-gpu"); // GPU 비활성화 (성능 개선)

		WebDriver driver = new ChromeDriver(options);
		/*
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));

		System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new");
		WebDriver driver = new ChromeDriver(options);

		 */

		List<Slope> slopeList = new ArrayList<>();

		try {
			driver.get("https://phoenixhnr.co.kr/static/pyeongchang/snowpark/slope-lift");

			String difficulty = null;

			WebElement tbody = driver.findElement(By.tagName("tbody"));

			List<WebElement> trList = tbody.findElements(By.tagName("tr"));
			for(WebElement tr:trList){

				// 가져온 tr에서 td 데이터 여러개를 추출한다.
				List<WebElement> tdList = tr.findElements(By.tagName("td"));

				// tdList가 길이가 7이면 난이도를 추출한다.
				if(tdList.size()==7){
					difficulty = tdList.get(0).getText();
					tdList.remove(0);// 데이터를 통일해서 코드의 반복을 줄이기 위함.
				}

				Slope slope = Slope.builder()
					.resortName("phyungchang")
					.slopeName(tdList.get(0).getText())
					.difficulty(difficulty)
					.weekly(tdList.get(4).getText().equals("-")?"Close":"Open")
					.nightTime(tdList.get(5).getText().equals("-")?"Close":"Open")
					.lateAtNight(tdList.get(5).getText().equals("-")?"Close":"Open")
					.build();

				slopeList.add(slope);
			}

			// 3개는 부대시설이므로 제외한다.
			for(int i=0; i<slopeList.size()-3; i++){

				Slope slope = slopeList.get(i);

				if (slopeService.findSlopeBySlopeName(slope.getSlopeName())==null){ // 1.신규 등록을 하는 경우 -> save
					slopeService.save(slope); // 문제가 발생할 경우, 롤백을 하고, 다음 슬로프를 가져올 것.
				}
				else{ // 2. 이미 등록된 경우 -> update
					slopeService.updateSlopeBySlopeName(slope.getSlopeName(),slope);
				}

			}


			ListOperations<String,String> listOperations = redisTemplate.opsForList();

			// 1. 02를 구독한 userId를 가져온다.
			List<String> pyeongchangQueueList = listOperations.range("o2",0,-1);

			WebClient webClient =
				WebClient.builder()
					.baseUrl("http://localhost:8000")
					.build();

			// 2. 구독자들의 메지지 큐에 알림을 넣어준다
			for(String userId : pyeongchangQueueList){
				listOperations.leftPush(userId+"MessageQueue","o2 alert!");
			}
			// 3. 접속한 구독자들에게는 실시간 알림을 전송한다.
			for(String userId: pyeongchangQueueList){
				String response = webClient
					.get()
					.uri(uriBuilder ->
						uriBuilder
							.path("/publish")
							.queryParam("userId",userId)
							.queryParam("type","phyungchang")
							.build()
					)
					.retrieve()
					.bodyToMono(String.class)
					.block()
					;
			}

		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally {
			driver.quit();
		}





	}
}
