package com.st.controller;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.st.frame.Join;
import com.st.frame.Search;
import com.st.frame.Service;
import com.st.frame.UserMoim;
import com.st.moim.Moim;
import com.st.util.FileSave;

@Controller
public class MoimController {
	@Resource(name="mservice")
	Service<String, Moim> service;
	
	@Resource(name="mservice")
	Search<String,Moim> search;
	
	@Resource(name="mservice")
	Join join;
	
	@Resource(name="mservice")
	UserMoim uMoim;
	
	@RequestMapping("/createmoim.st")
	public ModelAndView createmoim() {//move createmoim page
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		mv.addObject("centerpage","moim/create");
		return mv;
	}

	@RequestMapping("/createmoimimpl.st")
	public ModelAndView createmoimimpl(Moim moim,HttpServletRequest request) {//moim insert
		MultipartFile mp = moim.getMoimMultiImg();
		String moimImg= mp.getOriginalFilename();
		moim.setMoimImg(moimImg);
		
		//주소랑 상세주소 합쳐서 객체에 저장하기.
		String address = request.getParameter("address")+" ";
		address += request.getParameter("address2");
		moim.setPlace(address);
		
		//session id 가져오기
		HttpSession session = request.getSession();
		String userId = (String)session.getAttribute("userId");
		System.out.println(userId);
		moim.setUserId(userId);
		
		//상대경로로 가져오기
		String path = session.getServletContext().getRealPath("/");
		path += "img\\";
		
		FileSave.save(path, mp, moimImg);
		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		try {
			service.register(moim);
			mv.addObject("centerpage","center");
		} catch (Exception e) {
			e.printStackTrace();
			//fail 이면 조건 줘서 alert 뛰우기
			mv.addObject("fail","createfail");
			mv.addObject("centerpage","center");
		}
		
		return mv;
	}
	
	@RequestMapping("/moimdetail.st")
	public ModelAndView moimdetail(HttpServletRequest request,Map<String, String> map) {
		//select(id)로 하는데 moim id 넘겨줌
		
		String moimId = request.getParameter("id");
		Moim moim = null;
		Moim joinMoim = null;
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		HttpSession session = request.getSession();
		String userId = (String)session.getAttribute("userId");
		
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat today;
		
		today = new SimpleDateFormat("yyyy-MM-ddHH:mm");
		
		try {
			moim = service.get(moimId);
			System.out.println(moim);
			
			if(userId != null) {
				//신청 했는지 안했는지 검사하기. userId와 moimId 넘겨주기.
				map.put("userId", userId);
				map.put("moimId", moim.getMoimId());
				
				joinMoim = uMoim.checkJoin(map);
			}

			// Catch the data, time bug
			Date moimEdate = today.parse(moim.geteDate()+moim.geteTime());
			Date moimSdate = today.parse(moim.getsDate()+moim.getsTime());
			Date moimApplyEdate = today.parse(moim.getApplyEDate()+moim.getApplyETime());
			Date moimApplySdate = today.parse(moim.getApplySDate()+moim.getApplySTime());
			
			boolean eflag = date.getTime() < moimEdate.getTime();
			boolean sflag = date.getTime() < moimSdate.getTime();
			boolean aeflag = date.getTime() < moimApplyEdate.getTime();
			boolean asflag = date.getTime() < moimApplySdate.getTime();
			
			mv.addObject("eflag",eflag);
			mv.addObject("sflag",sflag);
			mv.addObject("aeflag",aeflag);
			mv.addObject("asflag",asflag);
			
			mv.addObject("joinCheckMoim",joinMoim);
			mv.addObject("moimdetail",moim);
			mv.addObject("userid",userId);
			mv.addObject("centerpage","moim/detail");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage","moim/detail");
		}
		
		return mv;
	}
	
	@RequestMapping("/moimlist.st")
	public ModelAndView moimlist(HttpServletRequest request) {
		//cmd 로 정보를 받아와서 c1,c2 구분 후 리스트 출력 해줘야함.
		//mapper.xml에서는 select 구문 2개 나눠서 써야할듯.아니면 하나로 써서 파라미터로 넘겨줘서 조건 줘도 될듯.
		//session에 moimId를 저장해놓거나 hidden으로 detail로 정보를 보내기.
		
		//카테고리 구분을 위해 받았다.
		String cmd = request.getParameter("cmd");
		
		ArrayList<Moim> list = null;
		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		try {
			list = search.search(cmd);
			
			mv.addObject("moimKind",cmd);
			mv.addObject("moimlist",list);
			mv.addObject("centerpage","moim/list");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage","moim/list");			
		}
		
		return mv;
	}
	
	@RequestMapping("/enjoylist.st")
	@ResponseBody
	public void enjoylist(HttpServletResponse response) {
		//카테고리 구분 객체 하기.				
		ArrayList<Moim> enjoyList = null;
		
		//JSON과 AJAX data 넘겨주기
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();
			//Data : from DB
			enjoyList = search.search("c1");
			
			JSONObject jo = null;
			JSONArray js = new JSONArray();
			
			for(int i=0;i<4;i++) {
				jo = new JSONObject();
				jo.put("moimId", enjoyList.get(i).getMoimId());
				jo.put("moimImg", enjoyList.get(i).getMoimImg());
				jo.put("title", enjoyList.get(i).getTitle());
				jo.put("sdate", enjoyList.get(i).getsDate());
				jo.put("edate", enjoyList.get(i).geteDate());
				jo.put("categoryKind", enjoyList.get(i).getCategoryKind());
				js.add(jo);
			}

			out.print(js.toJSONString());
			
			
		} catch (Exception e) {
			e.printStackTrace();		
		}
		
		
		out.close();
	}
	
	@RequestMapping("/studylist.st")
	@ResponseBody
	public void studylist(HttpServletResponse response) {
		//카테고리 구분 객체 하기.				
		ArrayList<Moim> studyList = null;
		
		//JSON과 AJAX data 넘겨주기
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();
			//Data : from DB
			studyList = search.search("c2");
			JSONObject jo = null;
			JSONArray js = new JSONArray();
			
			for(int i=0;i<4;i++) {
				jo = new JSONObject();
				jo.put("moimId", studyList.get(i).getMoimId());
				jo.put("moimImg", studyList.get(i).getMoimImg());
				jo.put("title", studyList.get(i).getTitle());
				jo.put("sdate", studyList.get(i).getsDate());
				jo.put("edate", studyList.get(i).geteDate());
				jo.put("categoryKind", studyList.get(i).getCategoryKind());
				js.add(jo);
			}
			
			out.print(js.toJSONString());
			
		} catch (Exception e) {
			e.printStackTrace();		
		}
		
		
		out.close();
	}
	
	@RequestMapping("/deletemoim.st")
	public String deletemoim(Moim moim,HttpServletRequest request,Map<String,String> map) {//moim insert
		//delete는 user/detail에서 실행해서 하는게 좋을 거 같다.
		HttpSession session = request.getSession();
		
		String userId = (String)session.getAttribute("userId");
		String cmd = request.getParameter("cmd");
		String moimId = request.getParameter("moimId");
		
		map.put("userId", userId);
		map.put("moimId", moimId);
		
		Moim deleteMoim = new Moim();
		deleteMoim.setUserId(userId);
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		try {
			if(cmd.equals("my")) {//개설한 모임 취소
				try {
					search.searchJoinMoim(userId);

					uMoim.delete(moimId);
					uMoim.deleteMoim(map);
					return "redirect:/mypage.st?cmd=my";
				}catch(Exception e) {					
					uMoim.deleteMoim(map);
					return "redirect:/mypage.st?cmd=my";
				}
			}else if(cmd.equals("join")) {//신청한 모임 취소
				System.out.println(userId);
				uMoim.deleteUser(map);
				return "redirect:/mypage.st?cmd=join";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/main.st";
		}
		return "redirect:/main.st";
	}
	
	@RequestMapping("/deleteusermoim.st")
	public String deleteusermoim(Moim moim,HttpServletRequest request,Map<String,String> map) {//moim insert
		//delete는 user/detail에서 실행해서 하는게 좋을 거 같다.
		HttpSession session = request.getSession();
		
		String userId = (String)session.getAttribute("userId");
		String moimId = request.getParameter("moimId");
		
		map.put("userId", userId);
		map.put("moimId", moimId);

		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		try {
				//신청한 모임 취소
				System.out.println(userId);
				uMoim.deleteUser(map);
				return "redirect:main.st";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:main.st";
		}
		
	}
	
	@RequestMapping("/updatemoim.st")
	public ModelAndView updatemoim(Moim moim,HttpServletRequest request) {//moim insert
		//update 수정
		//select 해와서 정보를 뿌려주고, 그걸로 세팅한다.
		//수정한 객체를 받아와서 바꿔준다.
		String moimId = request.getParameter("moimId");
		//origin Moim의 값을 저장해줌 => 이유: 수정할 때 이미지를 바꾸지 않을 때 원래의 값을 넣어주기 위함.
		HttpSession session = request.getSession();
		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
	
		try {
			moim = service.get(moimId);
			session.setAttribute("originMoim", moim);
			mv.addObject("moimdetail",moim);
			mv.addObject("centerpage","moim/create");
		} catch (Exception e) {
			e.printStackTrace();
			//fail 이면 조건 줘서 alert 뛰우기
			mv.addObject("fail","fail");
			mv.addObject("centerpage","moim/create");
		}
		
		return mv;
	}
	
	@RequestMapping("/updatemoimimpl.st")
	public ModelAndView updatemoimimpl(Moim moim,HttpServletRequest request) {//moim update
		
		HttpSession session = request.getSession();
		//origin Moim 객체 불러오기
		Moim originMoim = (Moim) session.getAttribute("originMoim");
		MultipartFile mp = moim.getMoimMultiImg();
		String moimImg= mp.getOriginalFilename();
		if(moimImg.equals("")) {
			moim.setMoimImg(originMoim.getMoimImg());
		}else {
			moim.setMoimImg(moimImg);	
			//상대경로로 가져오기
			String path = session.getServletContext().getRealPath("/");
			path += "img\\";
			
			FileSave.save(path, mp, moimImg);
		}
		
		
		//주소랑 상세주소 합쳐서 객체에 저장하기.
		String address = request.getParameter("address")+" ";
		address += request.getParameter("address2");
		moim.setPlace(address);
		
		//session id 가져오기

		String userId = (String)session.getAttribute("userId");
		System.out.println(userId);
		moim.setUserId(userId);

		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
	
		try {
			System.out.println(moim);
			if(originMoim.getMoimImg() != null) {//update Photo
				moim.setMoimId(originMoim.getMoimId());
				service.modify(moim);
			}else {//변경하지 않을때
				System.out.println(moim);
				service.modify(moim);				
			}
			mv.addObject("moimdetail",moim);
			mv.addObject("centerpage","moim/detail");
		} catch (Exception e) {
			e.printStackTrace();
			//fail 이면 조건 줘서 alert 뛰우기
			mv.addObject("fail","fail");
			mv.addObject("centerpage","moim/detail");
		}
		session.removeAttribute("originMoim");
		return mv;
	}
	
	@RequestMapping("/joinimpl.st")
	public String joinimpl(Map<String, Object> map,HttpServletRequest request) {//moim insert
		//user가 신청한 moimId와 그 userId를 USER_MOIM 테이블에 값을 넣어야한다.(신청한 모임)
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		
		HttpSession session = request.getSession();

		String user_id =(String) session.getAttribute("userId");
		String moim_id =(String) request.getParameter("moimId");
		
		map.put("user_id", user_id);
		map.put("moim_id", moim_id);
		
		try {
			System.out.println("start");
			join.join(map);
			System.out.println("success");
			return "redirect:main.st";
		} catch (Exception e) {
			System.out.println("fail");
			e.printStackTrace();
			//fail 이면 조건 줘서 alert 뛰우기
			return "redirect:main.st";
		}
	}
	
	
	
	
	
	
	
}
