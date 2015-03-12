<%@page contentType="text/html;charset=UTF-8" %>
<%
String billId=request.getParameter("billId");
String billMonth=request.getParameter("billMonth");
String fromDate=request.getParameter("fromDate").replaceAll("-", "");
String thruDate=request.getParameter("thruDate").replaceAll("-", "");
String busiType=request.getParameter("busiType");
String destId=request.getParameter("destId");
String startIndex=request.getParameter("startIndex");
String stopIndex=request.getParameter("stopIndex");
StringBuffer rest=new StringBuffer(com.asiainfo.billing.drquery.web.config.Config.getRest());
rest.append("query/common?billId=").append(billId);
rest.append("&billMonth=").append(billMonth);
rest.append("&fromDate=").append(fromDate);
rest.append("&thruDate=").append(thruDate);
rest.append("&busiType=").append(busiType);

if(destId!=null)rest.append("&destId=").append(destId);
if(startIndex!=null)rest.append("&startIndex=").append(startIndex);
if(stopIndex!=null)rest.append("&stopIndex=").append(stopIndex);

String result=com.asiainfo.billing.drquery.web.rest.RestManager.get(rest.toString());

result="{\"baseResponse\":{\"result\":\"success\",\"detail\":null,\"data\":{\"contents\":[[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409161\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409164\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409165\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409166\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409169\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409170\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409171\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409172\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409173\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409174\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409175\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409176\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409177\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409178\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409179\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409180\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409181\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409182\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409183\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"],[\"0\",\"91923\",\"0\",\"2006181\",\"合并后的超长话单\",\"20120413004912\",\"12\",\"210\",\"N-GCDR-CG06-erigcdr-20120413-00529550\",\"ddb12ae3\",\"210\",\"CMNET\",\"15\",\"3\",\"1494409184\",\"179\",\"-\",\"20120413005321\",\"2\",\"TD网络\",\"15721030570\",\"999999\",\"null\",\"null\",\"15\"]],\"startIndex\":81,\"stopIndex\":96,\"count\":16,\"totalCount\":96,\"stats\":{\"时长\":1440,\"通信费\":0,\"上行流量\":8824608,\"下行流量\":192593376},\"fields\":[\"延迟标志\",\"上行流量\",\"通信费\",\"下行流量\",\"是否是合并话单\",\"开始时间\",\"计费用户属性\",\"一级漫游地\",\"原文件名\",\"GGSN地址\",\"二级漫游地\",\"接入网络标识\",\"免费资源使用量\",\"用户计费类别\",\"计费标识\",\"GGSN中部分话单顺序号\",\"漫游类型\",\"批价处理时间\",\"合并话单标识\",\"接入网络类型\",\"用户号码\",\"服务识别码\",\"业务类型\",\"计费类别\",\"时长\"]}}}";

out.write(result);
%>
