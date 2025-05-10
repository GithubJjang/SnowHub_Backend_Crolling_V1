package com.snowhub.resort.domain.slope.o2;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
public class O2Scheduler {

	@Autowired
	private SlopeService slopeService;

	@Autowired
	@Qualifier("Master_redisTemplate")
	private RedisTemplate<String,String> redisTemplate;


	@Scheduled(fixedDelay = 600000)
	public void crollO2Slope() {
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

		try {
			driver.get("https://www.o2resort.com/SKI/slopeOpen.jsp");

			WebElement tbody = driver.findElement(By.tagName("tbody"));

			List<WebElement> trList = tbody.findElements(By.tagName("tr"));
			trList.remove(0);// 처음 0번째 인덱스는 무시

			// 1. Slope 난이도, 이름만 저장하는 리스트
			List<Slope> slopeList = new ArrayList<>();

			// 길이가 7이면 정상 8이면 난이도 섞인거
			String difficulty = null;
			for (WebElement row : trList) {

				List<WebElement> tdList = row.findElements(By.tagName("td"));

				if (tdList.size() == 7) { // 길이가 7이면 난이도 추출
					// 난이도 저장
					difficulty = tdList.get(0).getText();
					tdList.remove(0);// 코드의 일관성을 위해서 맨 앞은 난이도는 따로 저장하고 인덱스는 삭제한다.
				} else if (tdList.size() <= 5) {
					continue; // 부대시설은 데이터 길이가 5이하이므로 필터링을 한다.
				}

				Slope slope = Slope.builder()
					.resortName("O2")
					.slopeName(tdList.get(1).getText())
					.difficulty(difficulty)
					.build();

				slopeList.add(slope);
			}

			// 2. 오픈현황만 저장하는 리스트 1.과 순서는 동일하다.
			List<WebElement> openStatusList = driver.findElements(
				By.cssSelector("div.skiRightBox table.tblBasic tr.slopeLV1"));

			// 마지막에 슬로프 부대시설이 들어가서 제외할려고
			for(int i=0; i<slopeList.size()-1; i++){

				// slope 가져오기
				Slope slope = slopeList.get(i);

				// 오픈 현황에서 주간, 야간, 심야 가져오기
				WebElement openStatusElement = openStatusList.get(i);

				List<WebElement> openStatus = openStatusElement.findElements(By.tagName("img"));

				String weekly = Objects.equals(openStatus.get(0).getAttribute("src"),
					"https://www.o2resort.com/common/images/ski/icon_slope_closed.jpg") ? "Close" : "Open";

				String nightTime = Objects.equals(openStatus.get(1).getAttribute("src"),
					"https://www.o2resort.com/common/images/ski/icon_slope_closed.jpg") ? "Close" : "Open";

				String lateAtNight = Objects.equals(openStatus.get(2).getAttribute("src"),
					"https://www.o2resort.com/common/images/ski/icon_slope_closed.jpg") ? "Close" : "Open";


				slope.setWeekly(weekly);
				slope.setNightTime(nightTime);
				slope.setLateAtNight(lateAtNight);

				// 이미 등록이 됬는지 아닌지 확인을 해보자.
				if (slopeService.findSlopeBySlopeName(slope.getSlopeName())==null){ // 1.신규 등록을 하는 경우 -> save
					slopeService.save(slope); // 문제가 발생할 경우, 롤백을 하고, 다음 슬로프를 가져올 것.
				}
				else{ // 2. 이미 등록된 경우 -> update
					slopeService.updateSlopeBySlopeName(slope.getSlopeName(),slope);
				}
			}

			ListOperations<String,String> listOperations = redisTemplate.opsForList();

			// 1. 02를 구독한 userId를 가져온다.
			List<String> o2QueueList = listOperations.range("o2",0,-1);

			WebClient webClient =
				WebClient.builder()
					.baseUrl("http://localhost:8000")
					.build();

			// 2. 구독자들의 메지지 큐에 알림을 넣어준다
			for(String userId : o2QueueList){
				listOperations.leftPush(userId+"MessageQueue","o2 alert!");
			}
			// 3. 접속한 구독자들에게는 실시간 알림을 전송한다.
			for(String userId: o2QueueList){
				String response = webClient
					.get()
					.uri(uriBuilder ->
						uriBuilder
							.path("/publish")
							.queryParam("userId",userId)
							.queryParam("type","o2")
							.build()
					)
					.retrieve()
					.bodyToMono(String.class)
					.block()
					;

				}

		} catch (Exception e){
			e.printStackTrace();
		}
		finally {
			driver.quit();
		}

	}
}
