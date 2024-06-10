package net.musecom.spboard;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeleteController {
	
	@PostMapping("/del")
	public String delOk() {
		//아이디와 패스워드를 받아서
		//1. 비밀번호 검증
		//2. 틀리면
		//3. return 0
		//4. 성공
		//5. 파일db를 읽어서 파일을 삭제, 파일레코드 삭제, 게시판 레코드 삭제
		//6. return 1
		
		return "del";
	}
	
}
