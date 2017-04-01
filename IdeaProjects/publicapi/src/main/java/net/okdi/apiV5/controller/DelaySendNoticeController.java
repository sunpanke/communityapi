package net.okdi.apiV5.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.okdi.apiV2.service.SmsSendService;
import net.okdi.apiV2.service.TelephoneRewardService;
import net.okdi.apiV3.service.WalletNewService;
import net.okdi.apiV4.service.SendPackageService;
import net.okdi.apiV4.service.TaskMassNoticeService;
import net.okdi.apiV5.service.DelaySendNoticeService;
import net.okdi.apiV5.service.NewSendPhoneNoticeService;
import net.okdi.core.base.BaseController;
import net.okdi.core.util.PubMethod;
import net.okdi.core.util.service.ConstPool;
import net.okdi.mob.service.ExpGatewayService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

@Controller("newTaskMassNoticeController")
@RequestMapping("newTaskMassNotice")
public class DelaySendNoticeController extends BaseController{
	
	private static Logger logger = Logger.getLogger(DelaySendNoticeController.class);
	
	@Autowired
	DelaySendNoticeService delaySendNoticeService;
	@Autowired
	private ExpGatewayService expGatewayService;
	@Autowired
	private SmsSendService smsSendService;
	@Autowired
	private WalletNewService walletNewService;
	@Autowired
	private TelephoneRewardService telephoneRewardService;
	@Autowired
	private SendPackageService sendPackageService;//注入service
	private static Integer ONESENDNUM=500;  //群发通知每次提交上限
	@Autowired
	private NewSendPhoneNoticeService newSendPhoneNoticemsService;
	@Autowired
	ConstPool constPool;
	/**	 * 
	 * @api {post} /newTaskMassNotice/newSend 普通单延时发送
	 * @apiPermission user
	 * @apiDescription   zj
	 * @apiparam {String} accountId 钱包ID  (不能为空)
	 * @apiparam {String} phoneAndNum 手机号-编号-短信id-是否发送过-是否关注微信-运单号-网络name-姓名-parcelId|手机号和编号和短信id和是否发送过-是否关注微信-运单号-网络name-姓名-parcelId| 这样 isWx 0.关注 1.未关注  (不能为空)
	 * @apiparam {String} noticePhone 通知号码  (不能为空)
	 * @apiparam {Long} memberId 当前收派员的memberId  (不能为空)
	 * @apiparam {String} content 短信内容 (不能为空)
	 * @apiparam {String} flag 1：是群发短信  2:电话不通补发短信, 5:微信优先  (不能为空)
	 * @apiparam {String} pickupAddr 取件地址
	 * @apiparam {String} pickupTime 取件时间
	 * @apiparam {String} compName 快递公司
	 * @apiparam {String} version 版本号 如 5.0.0  (不能为空)
	 * @apiparam {String} system 操作系统  安卓 传 Android, 苹果  传 IOS(不能为空)
	 * @apiGroup 新版-短信
	 * @apiSampleRequest /newTaskMassNotice/newSend
	 * @apiSuccess {String} result true
	 * @apiError {String} result false
	 * @apiSuccessExample {json} Success-Response:
	 *     HTTP/1.1 200 OK
	 * 		{"success":"true"}
	 * @apiErrorExample {json} Error-Response:
	 *     HTTP/1.1 200 OK
	 *   	
	 * @apiVersion 5.0.0
	 */
	@ResponseBody
	@RequestMapping(value="/newSend", method = { RequestMethod.POST,RequestMethod.GET})
	public String newSend(String accountId,String phoneAndNum,String noticePhone,Long memberId,String content,
			Short flag, String pickupAddr, String pickupTime, String compName, String version, String system){
		logger.info("群发通知accountId:"+accountId+",phoneNumMsgId:"+phoneAndNum+",noticePhone:"+
				noticePhone+",memberId:"+memberId+",content:"+content+",flag:"+flag+" ,pickupAddr: "+pickupAddr+" ,pickupTime: "+pickupTime+" ,compName: "+compName);
		if(PubMethod.isEmpty(phoneAndNum)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.phoneAndNum","电话号码以及编号不能为空");
		}
		if(PubMethod.isEmpty(noticePhone)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.noticePhone","通知电话不能为空");
		}
		if(PubMethod.isEmpty(memberId)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.memberId","用户Id不能为空");
		}
		if(PubMethod.isEmpty(content)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.content","发送内容不能为空");
		}
		Short isNum=0; //0 不需要插编号 1需要插编号
		Map<String,String> receiverMap=new HashMap<String,String>();
		Integer contentLen=content.length();
		Integer contentLenNew=content.replaceAll("#编号#", "").length();
		if(contentLen!=contentLenNew){
			if(flag==1){
				isNum=1;
			}
		}
		content=content.trim();
		if(PubMethod.isEmpty(accountId)){
			accountId = this.walletNewService.getAccountId(memberId);//根据memberId查询accountId
		}
		/**start****增加对模板内容的修改的判断**********2016/12/27******************************/
		//根据memerid和content查询短信模板的信息，若果有，判断是状态是人工过还是系统过
		logger.info("新版对短信内容进行过滤，判断审核状态");
		if (!flag.equals((short)2)){//如果群呼 flag=2或者3的时候,模板不保存
			String returnjson=newSendPhoneNoticemsService.queryMsgTemplate
					(accountId,content,phoneAndNum,memberId,null,null,false,true,
							null,null,(short)1 , pickupTime,  pickupAddr,  compName,  flag,  noticePhone,  
							null,null, null);
			logger.info("新版短信模板审核返回的数据*******************returnjson: "+returnjson);
			///对返回的数据进行解析
			Map<String,Object>errormap = JSON.parseObject(String.valueOf(JSON.parseObject(returnjson)));
			if ("false".equals(errormap.get("success").toString())){
				logger.info("新版过滤返回的结果："+returnjson);
				return returnjson;
			}
		}
		/**stop****增加对模板内容的修改的判断****************************************/
		
		String[] arrs=phoneAndNum.split("\\|");
		//获取发送人的电话号码
		String memberInfo = this.expGatewayService.getMemberInfoById(memberId); 	//获取发送人的电话号码
		Map<String,Object> memberMap =  JSON.parseObject(String.valueOf(JSON.parseObject(memberInfo).get("data")));
		String memberPhone = memberMap.get("memberPhone").toString();
		
		String sendNum = telephoneRewardService.getSMSOrPhoneNum(accountId, memberPhone);			//郑炯的查询可用电话、短信和免费短信条数
		
		String dataAndStr=delaySendNoticeService.queryIsExist(memberId,phoneAndNum);
		JSONObject jsonAndMoile=JSONObject.parseObject(dataAndStr);
		Long count=0l;
		if(jsonAndMoile.getBoolean("success")){
			count=jsonAndMoile.getLong("data");
			String str=jsonAndMoile.getString("str");
			String[] mobileAndNums=str.split("\\|");
			for(String mobile:mobileAndNums){
				receiverMap.put(mobile.split("-")[0], mobile.split("-")[1]);
			}
		}
		logger.info("最后拼接起来的手机号的格式{receiverMap}: "+receiverMap);
		String balanceJson=delaySendNoticeService.queryBalance(sendNum, content,arrs,memberId,flag,count); 		//判断余额是否足够
		JSONObject jsons=JSONObject.parseObject(balanceJson);
		if(!jsons.getBooleanValue("success")){		
			return balanceJson;																			//如果失败就返回失败原因
		}
		String smsStartTime=constPool.getSmsStartTime();//短信开始时间
		String smsEndTime = constPool.getSmsEndTime();//短信结束时间
		String smsCause = constPool.getSmsCause();//提示语
		logger.info("派件群发通知短信的开始时间smsStartTime: "+smsStartTime+" ,结束时间smsEndTime: "+smsEndTime+" ,提示语smsCause: "+smsCause);
			
		String result="";
		Date date=new Date();					//夜间12点到早上7点不让发送短信
		String start=new SimpleDateFormat(smsStartTime).format(date);			
		String end=new SimpleDateFormat(smsEndTime).format(date);
		SimpleDateFormat sim=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			logger.info("开始时间"+start+"结束时间"+end);
			Date startTime=sim.parse(start);
			Date endTime=sim.parse(end);
			if(date.after(startTime)&&date.before(endTime)){
				Map<String,Object> errorMap = new HashMap<String,Object>();
				errorMap.put("success",false);
				errorMap.put("cause", smsCause);
				errorMap.put("flag", "");
				return JSON.toJSONString(errorMap);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(flag==2){//电话不通发短信
			String phoneAndWaybillNum="";
			int i=0;
			for(int j=0;j<arrs.length;j++){//arrs : 电话号-编号-msgId-isSendMsg-isWx-waybill-netName by 2016年12月23日 15:39:53
				arrs[j]=arrs[j]+"-tag";//加上标记为了数据传的为空,也不报空指针,也能拿到空数据
				String phone=arrs[j].split("-")[0];
				String isWx="0";
				String str=sendPackageService.queryOpenIdState(phone);
				logger.info("派件-语音----根据手机号查询该用户关注微信状态str: "+str+" ,该手机号phone: "+phone+" ,arrs[j]: "+arrs[j]);
				JSONObject jsonWx=JSONObject.parseObject(str);
				if(jsonWx.getBooleanValue("success")){
					//isWx="1";
					logger.info("派件--语音---解析得到的json串为jsonWx: "+jsonWx);
					JSONObject jsonObject = jsonWx.getJSONObject("data");
					if(!PubMethod.isEmpty(jsonObject.getString("isRemove"))){
						isWx = jsonObject.getString("isRemove");//0:关注, 1:手动解绑, 2:取消关注解绑
					}
				}
				////*****************************start**2016年12月23日 15:43:03**********************************************/
				String waybill="";
				String netName="";
				String name="";
				if(arrs[j].split("-").length>5){
					waybill = arrs[j].split("-")[5];
					netName = arrs[j].split("-")[6];
					if(arrs[j].split("-").length>7){
						name=arrs[j].split("-")[7];//姓名
					}
				}
				/************************************重新拼接串***为了延时发送 start****2016年12月24日 13:51:30***************************/
				String[] split = arrs[j].split("-");
				logger.info("taskMassNoticeController *********************flag="+flag+"短信切割串split: "+split);
				String ephone = split[0];//手机号
				String num = split[1];//运单号
				String msgId = split[2];//msgId
				String isSend = split[3];//是否发送过
				String parcelId = "";
				if(split.length>8){
					parcelId = split[8];//包裹id
				}
				String byParcelId = newSendPhoneNoticemsService.queryFirstMsgIdByParcelId(parcelId, flag);
				JSONObject parseObject = JSONObject.parseObject(byParcelId);
				String firstMsgId = parseObject.getString("firstMsgId");
				//电话号-编号-msgId-isSendMsg-isWx-wayBill-netName-姓名-parcelId
				// 手机号-编号-短信id-是否发送过-是否关注微信-运单号-网络name-姓名-parcelId
				/********************start***************************/
//				//ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId;
				// 去掉是因为不影响业务,现在包裹都是先生成的,都是去根据包裹id更改包裹状态,所以可以直接去掉 "-"+msgId+"-"+isSend+
				/*********************end***************************/
				if(i==0){
					phoneAndWaybillNum+=ephone+"-"+num+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
					i++;
				}else{
					phoneAndWaybillNum+="|"+ephone+"-"+num+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
				}
				/***********************************************end*****************2016年12月24日 13:51:36****************************/
				////*********************************end****************************************************/
				
			}
			content = content.replaceAll("#编号#", "");
			result = delaySendNoticeService.sendTaskPhoneNotice(accountId,phoneAndWaybillNum, noticePhone,memberId,content+" 快递员的号码为:"+noticePhone,flag,isNum, pickupAddr,pickupTime, compName, version, system);
		}else if(flag==1){ //仅发短信
			String phoneAndWaybillNum="";
			int i=0;
			for(int j=0;j<arrs.length;j++){
				arrs[j]=arrs[j]+"-tag";//加上标记为了数据传的为空,也不报空指针,也能拿到空数据
				String phone=arrs[j].split("-")[0];
				String isWx="0";
				String str=sendPackageService.queryOpenIdState(phone);
				logger.info("派件-----根据手机号查询该用户关注微信状态str: "+str+" ,该手机号phone: "+phone+" ,arrs[j]: "+arrs[j]);
				JSONObject jsonWx=JSONObject.parseObject(str);
				if(jsonWx.getBooleanValue("success")){
					//isWx="1";
					logger.info("派件-----解析得到的json串为jsonWx: "+jsonWx);
					JSONObject jsonObject = jsonWx.getJSONObject("data");
					if(!PubMethod.isEmpty(jsonObject.getString("isRemove"))){
						isWx = jsonObject.getString("isRemove");//0:关注, 1:手动解绑, 2:取消关注解绑
					}
				}
				////*****************************start**2016年12月23日 15:43:03**********************************************/
				String waybill="";//运单号
				String netName="";//网络名称
				String name="";//姓名
				if(arrs[j].split("-").length>5){
					waybill = arrs[j].split("-")[5];
					netName = arrs[j].split("-")[6];
					if(arrs[j].split("-").length>7){
						name=arrs[j].split("-")[7];//姓名
					}
				}
				
				/************************************重新拼接串***为了延时发送 start****2016年12月24日 13:51:30***************************/
				String[] split = arrs[j].split("-");
				logger.info("taskMassNoticeController *********************flag="+flag+"短信切割串split: "+split);
				String ephone = split[0];//手机号
				String num = split[1];//运单号
				String msgId = split[2];//msgId
				String isSend = split[3];//是否发送过
				String parcelId = "";
				if(split.length>8){
					parcelId = split[8];//包裹id
				}
				String byParcelId = newSendPhoneNoticemsService.queryFirstMsgIdByParcelId(parcelId, flag);
				JSONObject parseObject = JSONObject.parseObject(byParcelId);
				String firstMsgId = parseObject.getString("firstMsgId");
				////电话号-编号-msgId-isSendMsg-isWx-wayBill-netName-姓名-parcelId
				if(i==0){ 				//ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
					phoneAndWaybillNum+=ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
					i++;
				}else{
					phoneAndWaybillNum+="|"+ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
				}
				/***********************************************end*****************2016年12月24日 13:51:36****************************/
				
			}
			logger.info("原来发短信的串为{phoneAndNum}: "+phoneAndNum+" 发送短信拼接的串为{phoneAndWaybillNum}: "+phoneAndWaybillNum);
			result=this.delaySendNoticeService.sendTaskSms(accountId,content, phoneAndWaybillNum, memberId, false,true,null,"",isNum, flag, pickupAddr, pickupTime, compName, version,system);
		}else if(flag==5){//微信优先
			logger.info("派件-延时发送-微信优先 flag: "+flag+" ,拼接的字符串phoneAndNum: "+phoneAndNum);
			String phoneAndWaybillNum="";
			int i=0;
			Integer contentLeng=content.length();
			Integer contentLenNewg=content.replaceAll("#编号#", "").length();
			String bb = "0";
			if(contentLeng != contentLenNewg){//内容不相等, 插入编号了
				bb = "1";
			}
			//phoneNumMsgId:13426465659-26-258406143557632-0-1-70456105009568-百世-|13426465758-25-258406143557633-0-1-50272350207319-百世-
			logger.info("contentLeng: "+contentLeng+",contentLenNewg: "+contentLenNewg+",bb:"+bb);
			for(int j=0;j<arrs.length;j++){
				arrs[j]=arrs[j]+"-tag";//加上标记为了数据传的为空,也不报空指针,也能拿到空数据
				String phone=arrs[j].split("-")[0];
				String isWx="1";//默认未关注
				String str=sendPackageService.queryOpenIdState(phone);
				logger.info("根据手机号查询该用户关注微信状态str: "+str+" ,该手机号phone: "+phone+" ,arrs[j]: "+arrs[j]);
				JSONObject jsonWx=JSONObject.parseObject(str);
				if(jsonWx.getBooleanValue("success")){
					logger.info("解析得到的json串为jsonWx: "+jsonWx);
					JSONObject jsonObject = jsonWx.getJSONObject("data");
					if(!PubMethod.isEmpty(jsonObject.getString("isRemove"))){
						isWx = jsonObject.getString("isRemove");//0:关注, 1:手动解绑, 2:取消关注解绑
					}
				}
				////*****************************start**2016年12月23日 15:43:03**********************************************/
				//派件-延时发送-微信优先 flag: 5 ,拼接的字符串phoneAndNum: 13426465659-26-258406143557632-0-1-70456105009568-百世-|13426465758-25-258406143557633-0-1-50272350207319-百世-
				//phoneAndNum: 15810885211-31-258416327327744-0-1-17051071-百世-
				// 手机号-编号-短信id-是否发送过-是否关注微信-运单号-网络name-姓名-parcelId
				String waybill="";//运单号
				String netName="";//网络名称
				String name="";//姓名
				if(arrs[j].split("-").length>5){
					waybill = arrs[j].split("-")[5];
					netName = arrs[j].split("-")[6];
					if(arrs[j].split("-").length>7){
						name=arrs[j].split("-")[7];//姓名
					}
				}
				/************************************重新拼接串***为了延时发送 start****2016年12月24日 13:51:30***************************/
				String[] split = arrs[j].split("-");
				logger.info("taskMassNoticeController *********************flag="+flag+"微信优先切割串split: "+split);
				String ephone = split[0];//手机号
				String num = split[1];//运单号
				String msgId = split[2];//msgId
				String isSend = split[3];//是否发送过
				////电话号-编号-msgId-isSendMsg-isWx-wayBill-netName-姓名-parcelId
				// 手机号-编号-短信id-是否发送过-是否关注微信-运单号-网络name-姓名-parcelId
				String parcelId = "";
				if(split.length>8){
					parcelId = split[8];//包裹id
				}
				String byParcelId = newSendPhoneNoticemsService.queryFirstMsgIdByParcelId(parcelId, flag);
				JSONObject parseObject = JSONObject.parseObject(byParcelId);
				String firstMsgId = parseObject.getString("firstMsgId");
				if(i==0){
					phoneAndWaybillNum+=ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
					i++;
				}else{
					phoneAndWaybillNum+="|"+ephone+"-"+num+"-"+msgId+"-"+isSend+"-"+isWx+"-"+receiverMap.get(phone)+"-"+waybill+"-"+netName+"-"+name+"-"+parcelId+"-"+firstMsgId+"-tag";
				}
				/***********************************************end*****************2016年12月24日 13:51:36****************************/
			}
			logger.info("**************phoneAndWaybillNum: "+phoneAndWaybillNum);
			result=this.delaySendNoticeService.sendTaskSms(accountId,content, phoneAndWaybillNum, memberId,false,true,null,"",isNum, flag, pickupAddr,pickupTime, compName, version, system);//
		
			
			
		}
		return result;
	}
	/**	 * 
	 * @api {post} /newTaskMassNotice/newAliSend 淘宝单延时发送通知
	 * @apiPermission zj
	 * @apiDescription   zj
	 * @apiparam {String} accountId 钱包ID(不能为空)
	 * @apiparam {String} phoneAndNum 运单号和编号和短信id和是否发送过-网络name-netId-parcelId|运单号和编号和短信id和是否发送过-网络name-netId-parcelId| 这样 (不能为空) 
	 * @apiparam {String} noticePhone 通知号码(不能为空)
	 * @apiparam {Long} memberId 当前收派员的memberId(不能为空)
	 * @apiparam {String} content 短信内容(不能为空)
	 * @apiparam {String} flag 9：是群发短信(不能为空)
	 * @apiparam {String} timeStamp 防重复提交的唯一串
	 * @apiparam {String} pickupAddr 取件地址
	 * @apiparam {String} pickupTime 取件时间
	 * @apiparam {String} compName 快递公司
	 * @apiparam {String} version 版本号 5.0.0  (不能为空)
	 * @apiparam {String} system 操作系统  安卓 传 Android, 苹果  传 IOS(不能为空)
	 * @apiGroup 新版-短信
	 * @apiSampleRequest /newTaskMassNotice/newAliSend
	 * @apiSuccess {String} result true
	 * @apiError {String} result false
	 * @apiSuccessExample {json} Success-Response:
	 *     HTTP/1.1 200 OK
	 * 		{"success":"true"}
	 * @apiErrorExample {json} Error-Response:
	 *     HTTP/1.1 200 OK
	 *   	
	 * @apiVersion 5.0.0
	 */
	@ResponseBody
	@RequestMapping(value="/newAliSend", method={RequestMethod.POST, RequestMethod.GET})
	public String newAliSend(String accountId,String phoneAndNum,String noticePhone,Long memberId,String content,Short flag, 
			String timeStamp, String pickupAddr,String pickupTime, String compName, String version, String system){
		logger.info("新版派件淘宝单群发通知accountId:"+accountId+",phoneNumMsgId:"+phoneAndNum+",noticePhone:"+
				noticePhone+",memberId:"+memberId+",content:"+content+",flag:"+flag+" ,timeStamp: "+timeStamp+
				" ,pickupAddr: "+pickupAddr+" ,pickupTime: "+pickupTime+" ,compName: "+compName+" ,version: "+version);
		if(PubMethod.isEmpty(phoneAndNum)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.phoneAndNum","电话号码以及编号不能为空");
		}
		if(PubMethod.isEmpty(noticePhone)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.noticePhone","通知电话不能为空");
		}
		if(PubMethod.isEmpty(memberId)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.memberId","用户Id不能为空");
		}
		if(PubMethod.isEmpty(content)){
			return paramsFailure("SendPhoneNoticeController.sendPhoneNotice.content","发送内容不能为空");
		}
		Short isNum=0; //0 不需要插编号 1需要插编号
		
		Integer contentLen=content.length();
		Integer contentLenNew=content.replaceAll("#编号#", "").length();
		if(contentLen!=contentLenNew){
			if(flag==1){
				isNum=1;
			}
		}
		content=content.trim();
		
		if(PubMethod.isEmpty(accountId)){
			accountId = this.walletNewService.getAccountId(memberId);//根据memberId查询accountId
		}
		String[] arrs=phoneAndNum.split("\\|");
		//获取发送人的电话号码
		String memberInfo = this.expGatewayService.getMemberInfoById(memberId); 	//获取发送人的电话号码
		Map<String,Object> memberMap =  JSON.parseObject(String.valueOf(JSON.parseObject(memberInfo).get("data")));
		String memberPhone = memberMap.get("memberPhone").toString();
		
		String sendNum = telephoneRewardService.getSMSOrPhoneNum(accountId, memberPhone);			//郑炯的查询可用电话、短信和免费短信条数
		//********************************start****************************************************/
		Map<String,String> receiverMap=new HashMap<String,String>();
		String dataAndStr=delaySendNoticeService.queryIsExist(memberId,phoneAndNum);
		JSONObject jsonAndMoile=JSONObject.parseObject(dataAndStr);
		Long count=0l;
		if(jsonAndMoile.getBoolean("success")){
			count=jsonAndMoile.getLong("data");
			String str=jsonAndMoile.getString("str");
			String[] mobileAndNums=str.split("\\|");
			for(String mobile:mobileAndNums){
				receiverMap.put(mobile.split("-")[0], mobile.split("-")[1]);
			}
		}
		logger.info("最后拼接起来的手机号的格式{receiverMap}: "+receiverMap);
		/**********************************end********************************************************/
		Map<String,Object> resultMap=delaySendNoticeService.queryAliBalance(sendNum, content,arrs,memberId,flag, count); 		//判断余额是否足够
		logger.info("大于派件群发判断余额是否足够**************************resultMap: "+resultMap);
		if(!PubMethod.isEmpty(resultMap)){
			Object object = resultMap.get("cause");
			if(!"fff".equals(object)){
				return JSON.toJSONString(resultMap);
			}
		}
		String smsStartTime=constPool.getSmsStartTime();//短信开始时间
		String smsEndTime = constPool.getSmsEndTime();//短信结束时间
		String smsCause = constPool.getSmsCause();//提示语
		logger.info("派件群发通知短信的开始时间smsStartTime: "+smsStartTime+" ,结束时间smsEndTime: "+smsEndTime+" ,提示语smsCause: "+smsCause);
		
		String result="";
		Date date=new Date();					//夜间12点到早上7点不让发送短信
		String start=new SimpleDateFormat(smsStartTime).format(date);			
		String end=new SimpleDateFormat(smsEndTime).format(date);
		SimpleDateFormat sim=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			logger.info("开始时间"+start+"结束时间"+end);
			Date startTime=sim.parse(start);
			Date endTime=sim.parse(end);
			if(date.after(startTime)&&date.before(endTime)){
				Map<String,Object> errorMap = new HashMap<String,Object>();
				errorMap.put("success",false);
				errorMap.put("cause", smsCause);
				errorMap.put("flag","");
				return JSON.toJSONString(errorMap);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(flag==9){ //仅发短信
			String phoneAndWaybillNum="";
			int i=0;
			for(int j=0;j<arrs.length;j++){
				String wayBill=arrs[j].split("-")[0];//运单号
				
				/************************************重新拼接串***为了延时发送 start****2016年12月24日 13:51:30***************************/
				String[] split = (arrs[j]+"-tag").split("-");
				logger.info("taskMassNoticeController *********************flag="+flag+"短信切割串split: "+split);
				String num = split[1];//编号
				String msgId = split[2];//msgId
				String isSend = split[3];//是否发送过
				String netName = split[4];//网络名称
				String netId = split[5];//网络名称
				String parcelId = "";
				if(split.length>6){
					parcelId = split[6];//包裹id
				}
				String byParcelId = newSendPhoneNoticemsService.queryFirstMsgIdByParcelId(parcelId, flag);
				JSONObject parseObject = JSONObject.parseObject(byParcelId);
				String firstMsgId = parseObject.getString("firstMsgId");
				if(i==0){
					//运单号-编号-msgId-isSendMsg-netName-netId-parcelId
					phoneAndWaybillNum+=wayBill+"-"+num+"-"+msgId+"-"+isSend+"-"+netName+"-"+netId+"-"+resultMap.get(wayBill)+"-"+parcelId+"-"+firstMsgId+"-tag";//运单号-编号-msgId-是否第一次发送-网络名称-netId-是否扣费
					i++;
				}else{
					phoneAndWaybillNum+="|"+wayBill+"-"+num+"-"+msgId+"-"+isSend+"-"+netName+"-"+netId+"-"+resultMap.get(wayBill)+"-"+parcelId+"-"+firstMsgId+"-tag";
				}
				/***********************************************end*****************2016年12月24日 13:51:36****************************/
				
			}
			logger.info("阿里大于原来发短信的串为{phoneAndNum}: "+phoneAndNum+" 发送短信拼接的串为{phoneAndWaybillNum}: "+phoneAndWaybillNum);
			result=this.delaySendNoticeService.sendAliTaskSms(accountId,content, phoneAndWaybillNum, memberId, false,true,null,"",isNum, flag, pickupAddr,pickupTime, compName, version, system);
		}
		return result;
	}

}