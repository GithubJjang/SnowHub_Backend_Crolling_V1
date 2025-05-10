package com.snowhub.resort.dummy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class dummyController {
	@GetMapping("/")
	public String sayHello(HttpServletResponse response) {

		Cookie testCookie = new Cookie("test","IamCookie");
		testCookie.setDomain("");
		testCookie.setPath("/");
		testCookie.setSecure(true);

		Cookie testCookie2 = new Cookie("test2","IamFakeCookie");
		testCookie.setDomain("");
		testCookie.setPath("/");
		testCookie.setSecure(false);

		response.addCookie(testCookie);
		response.addCookie(testCookie2);



		return "hello home server!";
	}

	@GetMapping("/login")
	public RedirectView login(HttpServletResponse servletResponse) {
		System.out.println("로그인 성공적으로 접속");
		Cookie accessCookie = new Cookie("IdTokenCookie","IamIdToken");
		accessCookie.setDomain("localhost");
		accessCookie.setPath("/");
		accessCookie.setSecure(true); // 이 속성과
		accessCookie.setAttribute("SameSite", "None"); // 이 속성 추가

		Cookie refreshCookie = new Cookie("refreshTokenCookie","IamRefeshToken");
		refreshCookie.setDomain("localhost");
		refreshCookie.setPath("/");
		refreshCookie.setSecure(true); // 이 속성과
		refreshCookie.setAttribute("SameSite", "None"); // 이 속성 추가

		servletResponse.addCookie(accessCookie);
		servletResponse.addCookie(refreshCookie);

		RedirectView redirectView = new RedirectView();
		redirectView.setUrl("http://localhost:3002/success");


		System.out.println("login을 한 후, redirect를 합니다.");
		return redirectView;
	}
}
