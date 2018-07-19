package com.st.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.st.frame.Service;
import com.st.moim.Moim;
import com.st.util.FileSave;

@Controller
public class MoimController {
	
	@Resource(name="mservice")
	Service<String, Moim> service;
	
	@RequestMapping("/createmoim.st")
	public ModelAndView createmoim() {//move createmoim page
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		mv.addObject("centerpage","moim/create");
		return mv;
	}

	@RequestMapping("/createmoimimpl.st")
	public ModelAndView createmoimimpl(Moim moim) {//moim insert
		MultipartFile mp = moim.getmImg();
		String moimImg= mp.getOriginalFilename();
		moim.setMoimImg(moimImg);
		
		FileSave.save("C:\\springs\\Sticker\\web\\img\\", mp, moimImg);
		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		try {
			service.register(moim);
			mv.addObject("centerpage","main");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage","main");
		}
		
		return mv;
	}
	
}
