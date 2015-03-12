package com.asiainfo.billing.drquery.controller;

import com.asiainfo.billing.drquery.controller.reponse.DataResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author zhouquan3
 */
@Controller
@RequestMapping("/test")
public class TestController extends BaseController{
    private static Log log = LogFactory.getLog(TestController.class);
    
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView TestPost(HttpServletRequest req, HttpServletResponse resp) {
        return new ModelAndView("/test", new DataResponse("success").toMap());
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView TestGet(HttpServletRequest req, HttpServletResponse resp) {
        return new ModelAndView("/test", new DataResponse("success").toMap());
    }
    
    
    public static void main(String[] args){
    	String str = "{name: \"te\\\"st\"}";
    	JSONObject json = JSONObject.fromObject(str);
    	System.out.println(str);
    	System.out.println(json.get("name"));
    }
    
    
}
