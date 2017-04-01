package net.okdi.core.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 敏感词过滤
 * @Project：test
 * @Author : chenming
 * @Date ： 2014年4月20日 下午4:17:15
 * @version 1.0
 */
public class SensitivewordFilter {
	@SuppressWarnings("rawtypes")
	private Map sensitiveWordMap = null;
	//private static Map blackSensitiveWordMap = null;
	public static int minMatchTYpe = 1;      //最小匹配规则
	public static int maxMatchType = 2;      //最大匹配规则
	
//	public static String WHITESMSLIST = "white_sms_keyword.txt";
//	public static String BLACKSMSLIST = "black_sms_keyword.txt";
	
	/**
	 * 构造函数，初始化敏感词库
	 */
//	public SensitivewordFilter(String listName){
	public SensitivewordFilter(String listName,Set<String> set){ // 2015 08 20 kai.yang 增加一个set参数
		if("black".equalsIgnoreCase(listName)){
//			sensitiveWordMap = new SensitiveWordInit().initKeyWord(listName);
			sensitiveWordMap = new SensitiveWordInit().initKeyWord(set);
		}else if("white".equalsIgnoreCase(listName)){
//			sensitiveWordMap = new SensitiveWordInit().initKeyWord(listName);
			sensitiveWordMap = new SensitiveWordInit().initKeyWord(set);
		}else{
			throw new RuntimeException("请明确需要白名单white还是黑名单black！");
		}
	}
	
//	public SensitivewordFilter getSensitivewordFilter(String listName){
//		if("black".equalsIgnoreCase(listName)){
//			return new SensitivewordFilter("black");
//		}
//	}
	
	/**
	 * 判断文字是否包含敏感字符
	 * @author chenming 
	 * @date 2014年4月20日 下午4:28:30
	 * @param txt  文字
	 * @param matchType  匹配规则&nbsp;1：最小匹配规则，2：最大匹配规则
	 * @return 若包含返回true，否则返回false
	 * @version 1.0
	 */
	public boolean isContaintSensitiveWord(String txt,int matchType){
		txt = txt.toLowerCase();
		boolean flag = false;
		for(int i = 0 ; i < txt.length() ; i++){
			int matchFlag = this.CheckSensitiveWord(txt, i, matchType); //判断是否包含敏感字符
			if(matchFlag > 0){    //大于0存在，返回true
				flag = true;
				break;	//break;原先是没有的, 给加上了,原因是不能过滤 【】这两个特殊符号 2016年9月12日 10:37:51
			}
		}
		return flag;
	}
	
	/**
	 * 获取文字中的敏感词
	 * @author chenming 
	 * @date 2014年4月20日 下午5:10:52
	 * @param txt 文字
	 * @param matchType 匹配规则&nbsp;1：最小匹配规则，2：最大匹配规则
	 * @return
	 * @version 1.0
	 */
	public Set<String> getSensitiveWord(String txt , int matchType){
		txt = txt.toLowerCase();
		Set<String> sensitiveWordList = new HashSet<String>();
		
		for(int i = 0 ; i < txt.length() ; i++){
			int length = CheckSensitiveWord(txt, i, matchType);    //判断是否包含敏感字符
			if(length > 0){    //存在,加入list中
				sensitiveWordList.add(txt.substring(i, i+length));
				i = i + length - 1;    //减1的原因，是因为for会自增
			}
		}
		
		return sensitiveWordList;
	}
	
	/**
	 * 替换敏感字字符
	 * @author chenming 
	 * @date 2014年4月20日 下午5:12:07
	 * @param txt
	 * @param matchType
	 * @param replaceChar 替换字符，默认*
	 * @version 1.0
	 */
	public String replaceSensitiveWord(String txt,int matchType,String replaceChar){
		txt = txt.toLowerCase();
		String resultTxt = txt;
		Set<String> set = getSensitiveWord(txt, matchType);     //获取所有的敏感词
		Iterator<String> iterator = set.iterator();
		String word = null;
		String replaceString = null;
		while (iterator.hasNext()) {
			word = iterator.next();
			replaceString = getReplaceChars(replaceChar, word.length());
			resultTxt = resultTxt.replaceAll(word, replaceString);
		}
		
		return resultTxt;
	}
	
	/**
	 * 获取替换字符串
	 * @author chenming 
	 * @date 2014年4月20日 下午5:21:19
	 * @param replaceChar
	 * @param length
	 * @return
	 * @version 1.0
	 */
	private String getReplaceChars(String replaceChar,int length){
		String resultReplace = replaceChar;
		for(int i = 1 ; i < length ; i++){
			resultReplace += replaceChar;
		}
		
		return resultReplace;
	}
	
	/**
	 * 检查文字中是否包含敏感字符，检查规则如下：<br>
	 * @author chenming 
	 * @date 2014年4月20日 下午4:31:03
	 * @param txt
	 * @param beginIndex
	 * @param matchType
	 * @return，如果存在，则返回敏感词字符的长度，不存在返回0
	 * @version 1.0
	 */
	@SuppressWarnings({ "rawtypes"})
	public int CheckSensitiveWord(String txt,int beginIndex,int matchType){
		txt = txt.toLowerCase();
		boolean  flag = false;    //敏感词结束标识位：用于敏感词只有1位的情况
		int matchFlag = 0;     //匹配标识数默认为0
		char word = 0;
		Map nowMap = sensitiveWordMap;
		//String regEx = "[\\u4e00-\\u9fa5]";
		String regEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";
		Pattern p = Pattern.compile(regEx);
		for(int i = beginIndex; i < txt.length() ; i++){
			word = txt.charAt(i);
			Matcher m = p.matcher(String.valueOf(word)); 
			if(m.matches()){
				matchFlag++;
				//continue;// continue这个是随着isContaintSensitiveWord 方法中的 添加break去掉的, 2016年9月12日 10:39:21(原因是过滤不掉【】这两个特殊符号)
			}
			nowMap = (Map) nowMap.get(word);     //获取指定key
			if(nowMap != null){     //存在，则判断是否为最后一个
				matchFlag++;     //找到相应key，匹配标识+1 
				if("1".equals(nowMap.get("isEnd"))){       //如果为最后一个匹配规则,结束循环，返回匹配标识数
					flag = true;       //结束标志位为true   
					if(SensitivewordFilter.minMatchTYpe == matchType){    //最小规则，直接返回,最大规则还需继续查找
						break;
					}
				}
			}
			else{     //不存在，直接返回
				break;
			}
		}
		if(matchFlag < 1 || !flag){        //长度必须大于等于1，为词 
			matchFlag = 0;
		}
		return matchFlag;
	}
	
//	public static void main(String[] args) {
//		
//		SensitivewordFilter blackfilter = new SensitivewordFilter("black");
//		System.out.println(blackfilter.sensitiveWordMap);
//		System.out.println("敏感词的数量：" + blackfilter.sensitiveWordMap.size());
////		String string = "太多的伤感情怀也许只局限于饲养基地 荧幕中的情节，主人公尝试着去用某种方式渐渐的很潇洒地释自杀指南怀那些自己经历的伤感。"
////						+ "然后法.轮功 我们的扮演的角色就是跟随着主人公的喜红客联盟 怒哀乐而过于牵强的把自己的情感也附加于银幕情节中，然后感动就流泪，"
////						+ "难过就躺在 某一个人的怀里尽情的阐述心扉或者手机卡复制器一个人一杯红酒一部电影在夜三级片 深人静的晚上，关上电话静静的发呆着。";
//		String string = "中文测试快递";
//		string = "COM递快件";
//		//string = "dsafsfd";
//		System.out.println("待检测语句字数：" + string.length());
//		//long beginTime = System.currentTimeMillis();
//		Set<String> set = blackfilter.getSensitiveWord(string, 1);
//		boolean bblack = blackfilter.isContaintSensitiveWord(string, 1);
//		System.out.println("黑名单是否包含："+bblack);
//		//long endTime = System.currentTimeMillis();
//		System.out.println("语句中包含敏感词的个数为：" + set.size() + "。包含：" + set);
//		//System.out.println("总共消耗时间为：" + (endTime - beginTime));
//		
//		System.out.println("****************************");
//		SensitivewordFilter whitefilter = new SensitivewordFilter("white");
//		System.out.println(whitefilter.sensitiveWordMap);
//		System.out.println("白名单敏感词的数量：" + whitefilter.sensitiveWordMap.size());
//		string = "炸枪aaa强奸快递快件";
//		string = "炸枪aaa强奸";
//		System.out.println("待检测语句字数：" + string.length());
//		set = whitefilter.getSensitiveWord(string, 1);
//		boolean bwhite = whitefilter.isContaintSensitiveWord(string, 1);
//		System.out.println(bwhite);
//		System.out.println("语句中包含敏感词的个数为：" + set.size() + "。包含：" + set);
//	}
}