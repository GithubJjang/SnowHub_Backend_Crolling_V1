package com.snowhub.resort.domain.slope.jisan;

import java.time.Duration;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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
public class JisanScheduler {

	@Autowired
	private SlopeService slopeService;

	@Autowired
	@Qualifier("Master_redisTemplate")
	private RedisTemplate<String,String> redisTemplate;

	@Scheduled(fixedDelay = 600000)
	public void crollJisanSlope(){
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
		try{

			driver.get("https://www.jisanresort.co.kr/w/ski/slopes/info.asp");

			List<WebElement> trList;

			List<String> nameList = new ArrayList<>();
			List<String> diffcultyList = new ArrayList<>();


			WebElement thead = driver.findElement(By.tagName("thead"));
			trList = thead.findElements(By.tagName("tr"));

			// 1. 이름 추출
			WebElement nameTr = trList.get(0);

			List<WebElement> thNameList = nameTr.findElements(By.tagName("th"));
			for(WebElement e:thNameList){
				nameList.add(e.getText());
			}
			nameList.remove(0);// 첫 인덱스 값은 "구분"이라서 지운다.
			
			// 2. 난이도 추출
			WebElement difficultyTr = trList.get(1);

			List<WebElement> thdifficultyList =difficultyTr.findElements(By.tagName("th"));
			for(WebElement e:thdifficultyList){
				diffcultyList.add(e.getText());
			}
			// 이름은 nameList에 들어있고, 난이도는 difficultyList에 들어있다.


			trList.clear();

			WebElement tbody = driver.findElement(By.tagName("tbody"));
			trList = tbody.findElements(By.tagName("tr"));


			// 주간 정보를 담은 리스트
			WebElement weeklys = trList.get(0);
			List<WebElement> weeklyList = weeklys.findElements(By.tagName("td"));
			weeklyList.remove(0);

			// 야간 정보를 담은 리스트
			WebElement nightTimes = trList.get(1);
			List<WebElement> nightTimeList = nightTimes.findElements(By.tagName("td"));
			nightTimeList.remove(0);

			// 심야 정보를 담은 리스트
			WebElement lastAtNights = trList.get(2);
			List<WebElement> lateAtNightList = lastAtNights.findElements(By.tagName("td"));
			lateAtNightList.remove(0);

			// 어차피 이름개수만큼만 정보양이 각각 들어 있다.

			/*
			WebElement e = weeklyList.get(1);
			List<WebElement> we = e.findElements(By.tagName("img"));
			System.out.println(we.get(0).getAttribute("src"));
			 */
			for(int i=0; i<nameList.size(); i++){

				String weekly = Objects.equals(
					weeklyList.get(i).findElements(By.tagName("img")).get(0).getAttribute("src"),
					"https://www.jisanresort.co.kr/w/asset/images/sub/ski/slope_close.png") ? "Close" : "Open";

				String nightTime = Objects.equals(
					nightTimeList.get(i).findElements(By.tagName("img")).get(0).getAttribute("src"),
					"https://www.jisanresort.co.kr/w/asset/images/sub/ski/slope_close.png") ? "Close" : "Open";

				String lateAtNight = Objects.equals(
					lateAtNightList.get(i).findElements(By.tagName("img")).get(0).getAttribute("src"),
					"https://www.jisanresort.co.kr/w/asset/images/sub/ski/slope_close.png") ? "Close" : "Open";

				Slope slope = Slope.builder()
					.resortName("jisan")
					.slopeName(nameList.get(i))
					.difficulty(diffcultyList.get(i))
					.weekly(weekly)
					.nightTime(nightTime)
					.lateAtNight(lateAtNight)
					.build()
					;

				// 이미 등록이 됬는지 아닌지 확인을 해보자.
				if (slopeService.findSlopeBySlopeName(slope.getSlopeName())==null){ // 1.신규 등록을 하는 경우 -> save
					slopeService.save(slope); // 문제가 발생할 경우, 롤백을 하고, 다음 슬로프를 가져올 것.
				}
				else{ // 2. 이미 등록된 경우 -> update
					slopeService.updateSlopeBySlopeName(slope.getSlopeName(),slope);
				}

			}

			ListOperations<String,String> listOperations = redisTemplate.opsForList();

			// 1. jisan를 구독한 userId를 가져온다.
			List<String> jisanQueueList = listOperations.range("jisan",0,-1);

			WebClient webClient =
				WebClient.builder()
					.baseUrl("http://localhost:8000")
					.build();

			// 2. 구독자들의 메지지 큐에 알림을 넣어준다
			for(String userId : jisanQueueList){
				listOperations.leftPush(userId+"MessageQueue","jisan alert!");
			}
			// 3. 접속한 구독자들에게는 실시간 알림을 전송한다.
			for(String userId: jisanQueueList) {
				String response = webClient
					.get()
					.uri(uriBuilder ->
						uriBuilder
							.path("/publish")
							.queryParam("userId", userId)
							.queryParam("type", "jisan")
							.build()
					)
					.retrieve()
					.bodyToMono(String.class)
					.block();

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
