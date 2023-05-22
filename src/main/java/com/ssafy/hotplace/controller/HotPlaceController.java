package com.ssafy.hotplace.controller;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.tomcat.util.file.ConfigurationSource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ssafy.hotplace.model.HotPlaceDto;
import com.ssafy.hotplace.model.service.IHotPlaceService;
import com.ssafy.util.PageNavigation;

@RestController
@CrossOrigin("*")
@RequestMapping("/hotplace")
public class HotPlaceController {

	private final Logger logger = LoggerFactory.getLogger(HotPlaceController.class);
	private static final String SUCCESS = "success";
	private static final String FAIL = "fail";
    
	private ServletContext servletContext;
	private IHotPlaceService hotplaceService;

	public HotPlaceController(ServletContext servletContext, IHotPlaceService hotplaceService) {
		super();
        this.servletContext = servletContext;
		this.hotplaceService = hotplaceService;
	}

	/*
	 * @GetMapping("/write") public String write(@RequestParam Map<String, String>
	 * map, Model model) { logger.debug("write call parameter {}", map);
	 * model.addAttribute("pgno", map.get("pgno")); model.addAttribute("key",
	 * map.get("key")); model.addAttribute("word", map.get("word")); return
	 * "board/write"; }
	 */

	@PostMapping("/")
	public ResponseEntity<?> write(@RequestBody HotPlaceDto hotplaceDto, @RequestParam("upfile") MultipartFile file) {
		logger.debug("write hotplaceDto : {}", hotplaceDto);
		HttpStatus status = null;
		try {
            //		FileUpload 관련 설정.
		logger.debug("MultipartFile.isEmpty : {}", file.isEmpty());
		if (!file.isEmpty()) {
			String realPath = servletContext.getRealPath("/upload");
			// String realPath = servletContext.getRealPath("/resources/img");
			String today = new SimpleDateFormat("yyMMdd").format(new Date());
			String saveFolder = realPath + File.separator + today;
			logger.debug("저장 폴더 : {}", saveFolder);
			File folder = new File(saveFolder);
			if (!folder.exists())
				folder.mkdirs();
				String originalFileName = file.getOriginalFilename();
				if (!originalFileName.isEmpty()) {
					String saveFileName = UUID.randomUUID().toString()
							+ originalFileName.substring(originalFileName.lastIndexOf('.'));
					hotplaceDto.setSaveFolder(today);
					hotplaceDto.setSaveFile(saveFileName);
					hotplaceDto.setOriginalFile(originalFileName);
					logger.debug("원본 파일 이름 : {}, 실제 저장 파일 이름 : {}", file.getOriginalFilename(), saveFileName);
					file.transferTo(new File(folder, saveFileName));
				}
		}
			hotplaceService.write(hotplaceDto);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스 쓰기 실패 : {}", e);
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(status);
	}

	@GetMapping("/")
	public ResponseEntity<?> list(@RequestParam Map<String, String> map) {
		logger.debug("list parameter pgno : {}", map);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
		List<HotPlaceDto> list = null;
		try {
			list = hotplaceService.hotplaceList(map);
            List<UrlResource> fileURLs = new ArrayList<>(); // 핫플레이스 리스트 각각에 해당하는 이미지 경로 리스트
            for(HotPlaceDto h : list){ // 각 핫플레이스 게시글 마다 이미지 경로 가져오는 코드
                if (!h.getOriginalFile().isEmpty()) { // 업로드한 파일이 존재하면
                    String realPath = servletContext.getRealPath("/upload");
                    // String realPath = servletContext.getRealPath("/resources/img");
                    String today = h.getSaveFolder(); // 저장된 폴더 이름
                    String saveFolder = realPath + File.separator + today; // 전체 경로 찾기 위함
                    logger.debug("저장 폴더 : {}", saveFolder);
                    String path = saveFolder + File.separator + h.getSaveFile();
                    fileURLs.add(new UrlResource("file:" + path));
                }
            }
			PageNavigation pageNavigation = hotplaceService.makePageNavigation(map);
			resultMap.put("list",list);
            resultMap.put("urls",fileURLs);
			resultMap.put("navigation", pageNavigation);
			resultMap.put("pgno", map.get("pgno"));
			resultMap.put("key", map.get("key"));
			resultMap.put("word", map.get("word"));
			resultMap.put("message", SUCCESS);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스  목록 불러오기 실패 : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(resultMap, status);
	}

	@GetMapping("/{hotplaceno}")
	public ResponseEntity<?> view(@PathVariable("hotplaceno") int hotplaceNo, @RequestParam Map<String, String> map) {
		logger.debug("view hotplaceNo : {}", hotplaceNo);
		HotPlaceDto hotplaceDto = null;
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
        UrlResource url = null;
		try {
			hotplaceDto = hotplaceService.detail(hotplaceNo);
            if (!hotplaceDto.getOriginalFile().isEmpty()) { // 업로드한 파일이 존재하면
                String realPath = servletContext.getRealPath("/upload");
                // String realPath = servletContext.getRealPath("/resources/img");
                String today = hotplaceDto.getSaveFolder(); // 저장된 폴더 이름
                String saveFolder = realPath + File.separator + today; // 전체 경로 찾기 위함
                logger.debug("저장 폴더 : {}", saveFolder);
                String path = saveFolder + File.separator + hotplaceDto.getSaveFile();
                url = new UrlResource("file:" + path);
            }
			resultMap.put("hotplace", hotplaceDto);
            resultMap.put("url", url);
			resultMap.put("pgno", map.get("pgno"));
			resultMap.put("key", map.get("key"));
			resultMap.put("word", map.get("word"));
			resultMap.put("message", SUCCESS);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스  상세보기 실패 : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		logger.debug("view hotplace : {}", hotplaceDto);
		return new ResponseEntity<>(resultMap, status);
	}

	@PutMapping("/")
	public ResponseEntity<?> modify(@RequestBody HotPlaceDto hotplaceDto, @RequestParam Map<String, String> map){
		logger.debug("modify hotplaceDto : {}", hotplaceDto);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
		try {
			hotplaceService.update(hotplaceDto);
			hotplaceDto = hotplaceService.detail(hotplaceDto.getHotplaceNo());
			resultMap.put("hotplace", hotplaceDto);
			resultMap.put("pgno", map.get("pgno"));
			resultMap.put("key", map.get("key"));
			resultMap.put("word", map.get("word"));
			resultMap.put("message", SUCCESS);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스  수정 실패 : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(resultMap, status);
	}

	//핫플레이스  삭제 map 매핑 관련 처리..
	@DeleteMapping("/{hotplaceno}")
	public ResponseEntity<?> delete(@PathVariable("hotplaceno") int hotplaceNo, @RequestParam Map<String, String> map) {
		logger.debug("delete hotplaceNo : {}", hotplaceNo);
		// logger.debug("delete hotplaceNo : {}", map);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
		try {
			hotplaceService.delete(hotplaceNo);
			// resultMap.put("pgno", map.get("pgno"));
			// resultMap.put("key", map.get("key"));
			// resultMap.put("word", map.get("word"));
			resultMap.put("message", SUCCESS);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스  삭제 실패 : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(resultMap, status);
	}


	//수정할 필요가 있을 듯함
	@GetMapping("/recommend/{hotplaceNo}")
	public ResponseEntity<?> recommend(@PathVariable("hotplaceNo") int hotplaceNo, @RequestParam Map<String, String> map){
		logger.debug("recommend hotplace : {}", hotplaceNo);
		logger.debug("recommend hotplace : {}", map);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
		try {
			hotplaceService.recommend(hotplaceNo);
			resultMap.put("message", SUCCESS);
			status = HttpStatus.OK;
		} catch (Exception e) {
			logger.error("핫플레이스  추천 실패 : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(resultMap, status);
	}

}
