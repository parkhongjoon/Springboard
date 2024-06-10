package net.musecom.spboard;

import java.io.File;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import net.musecom.spboard.dao.UploadDao;
import net.musecom.spboard.dto.UploadFileDto;
import net.musecom.spboard.service.SpGetContentService;
import net.musecom.spboard.service.SpGetListService;
import net.musecom.spboard.service.SpSetContentService;
import net.musecom.spboard.service.TrashFileDel;

@Controller
public class BbsController {
   
	@Autowired
	SpGetListService getList;
	
	@Autowired
	SpGetContentService getContent;
	
	//insert
	@Autowired
	SpSetContentService setContent;
	
	@Autowired
	ServletContext servletContext;
	
	@Autowired
	UploadFileDto uploadFileDto;
	
	@Autowired
	UploadDao uploadDao;
	
	@Autowired
	TrashFileDel trashFileDel;
	
	@RequestMapping("/list")
	public String list(
			@RequestParam(value="cpg", defaultValue="1") int cpg, 
			@RequestParam(value="searchname", defaultValue="") String searchname,
			@RequestParam(value="searchvalue", defaultValue="") String searchvalue,
			Model model) {
		System.out.println("list() 실행됨");
        model.addAttribute("cpg" , cpg);
        model.addAttribute("searchname", searchname);
        model.addAttribute("searchvalue", searchvalue);
        getList.excute(model);
        trashFileDel.delCom();
        
		return "list";
	}
	
	
	@RequestMapping("/contents")
	public String contents(HttpServletRequest request, HttpServletResponse response, Model model) {
	
		System.out.println("contents() 실행됨");
		
		//조회수 증가 판단
		boolean increaseHit = true;
		Cookie[] cookies = request.getCookies();
		if(cookies != null) {
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals("addHit_" + request.getParameter("id")) && cookie.getValue().equals("hit")) {
					increaseHit = false;
					break;
				}
			}
		}

		if(increaseHit) {  //조회수 증가 로직
			model.addAttribute("increaseHit", true);
			Cookie hitCookie = new Cookie("addHit_"+request.getParameter("id"), "hit");
			hitCookie.setMaxAge(60*60); //1시간  24*60*60 하루
			hitCookie.setPath("/"); //모든 루트에서 쿠키가 유효 함.
			response.addCookie(hitCookie); //쿠키 저장
		}else {
			model.addAttribute("increaseHit", false);
		}
		
		model.addAttribute("req", request);
		getContent.excute(model);
		return "contents";
	}
	
	@GetMapping("/write")
	public String write(Model model) {
		System.out.println("write() 실행됨");
		String upDir = servletContext.getRealPath("/resources/");
		System.out.println(upDir);
		
		String imnum = UUID.randomUUID().toString();
		model.addAttribute("imnum", imnum);
		return "write";
	}
	
	@PostMapping("/write")
	public String writeok(HttpServletRequest request, HttpServletResponse response, Model model) {
		
		System.out.println("writeok() 실행됨");
		model.addAttribute("request", request);
		setContent.excute(model);
		return "redirect:list";
	}
	
	@GetMapping("/rewrite")
	public String rewrite(HttpServletRequest request, HttpServletResponse response, Model model) {
		System.out.println("rewrite() 실행됨");
		String upDir = servletContext.getRealPath("/resources/");
		System.out.println(upDir);
		
		String imnum = UUID.randomUUID().toString();
		
		model.addAttribute("refid", request.getParameter("refid"));
		model.addAttribute("depth", request.getParameter("depth"));
		model.addAttribute("renum", request.getParameter("renum"));
		model.addAttribute("imnum", imnum);
		return "rewrite";
	}
	
	@PostMapping("/rewrite")
	public String rewriteok(HttpServletRequest request, HttpServletResponse response, Model model) {
		
		System.out.println("writeok() 실행됨");
		model.addAttribute("request", request);
		setContent.excute(model);
		return "redirect:list";
	}
	
	
	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response,Model model) {
		System.out.println("edit() 실행됨");
		model.addAttribute("req", request);
		model.addAttribute("increaseHit", false);
		
		getContent.excute(model);		
		return "edit";
	}
	
	@PostMapping("/edit")
	public String editok(Model model) {
	    //mapper 확인
		//request값을 dto에 담아 
		//SpSetEditService 실행해서 db 에 값을 넣어줌
		//parameter로 page를 갖고 있다가
		//return "redirect:list?cpg="+cpg;
		System.out.println("edit() 실행됨");
		
		return "edit";
	}
	
	@PostMapping("/upload")
	@ResponseBody
	public ResponseEntity<?> handleImageUpload(
			@RequestParam("file") MultipartFile uploadFile,
			@RequestParam("imnum") String imnum){
		if(!uploadFile.isEmpty()) {
			try {
				//파일정보 추출
				String oFilename = uploadFile.getOriginalFilename();
				
				//확장자 추출
				String ext = oFilename.substring(oFilename.lastIndexOf(".") + 1).toLowerCase();
				
				//새파일 
				String nFilename = Long.toString(System.currentTimeMillis() / 1000L) + "." + ext;
				
				//파일크기
				long filesize = uploadFile.getSize();
				
				//추후 로그인 연동되면 회원아이디로 등록예정
				String userid = "guest";
				
				//경로설정
				String uploadDir = servletContext.getRealPath("/resources/upload/");
				
				//업로드
				File serverFile = new File(uploadDir + nFilename);
				uploadFile.transferTo(serverFile);
				
				//데이터베이스 저장
				uploadFileDto.setExt(ext);
				uploadFileDto.setFilesize(filesize);
				uploadFileDto.setImnum(imnum);
				uploadFileDto.setNfilename(nFilename);
				uploadFileDto.setOfilename(oFilename);
				uploadFileDto.setUserid(userid);
				
				uploadDao.insertFile(uploadFileDto);
				String json = "{\"url\":\"" + "/spboard/res/upload/" + nFilename + "\"}";
				return ResponseEntity.ok().body(json);
				
			}catch(Exception e) {
				e.printStackTrace();
				return ResponseEntity.badRequest().body("upload Error");
			}
			
		}else {
		   return ResponseEntity.badRequest().body("noFile");
		}   
	}
}
