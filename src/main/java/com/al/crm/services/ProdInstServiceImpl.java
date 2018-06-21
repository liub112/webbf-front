package com.al.crm.services;

import com.al.common.exception.BaseException;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("intf.prodInstService")
public class ProdInstServiceImpl {
	private static Logger LOG = LoggerFactory
			.getLogger(ProdInstServiceImpl.class);


	public String test(){
		return "test";
	}

}
