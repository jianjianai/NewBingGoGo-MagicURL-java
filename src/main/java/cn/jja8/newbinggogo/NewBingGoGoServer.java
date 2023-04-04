package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NewBingGoGoServer extends NanoWSD {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    public static void main(String[] args) {
        if(args.length<1){
            System.err.print("需要指定运行端口号！");
            return;
        }
        try{
            int porint = Integer.parseInt(args[0]);
            System.out.println("程序已在"+porint+"端口上启动.");
            new NewBingGoGoServer(porint).start(5000,false);
        }catch(Throwable s){
            s.printStackTrace();
        }
    }
    public NewBingGoGoServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String ip = session.getHeaders().get("x-forwarded-for");
        if (ip==null){
            ip = "未知ip地址";
        }
        ip = new Date()+":"+ip;

        String url = session.getUri();
        if(url.equals("/sydney/ChatHub")){
            System.out.println(ip+":创建魔法聊天连接");
            return super.serve(session);
        }
        if(url.equals("/turing/conversation/create")){//创建聊天
            System.out.println(ip+":请求创建聊天");
            return goUrl(session,"https://www.bing.com/turing/conversation/create");
        }
        if(url.equals("/msrewards/api/v1/enroll")){//加入候补
            System.out.println(ip+":请求加入候补");
            return goUrl(session,"https://www.bing.com/msrewards/api/v1/enroll?"+session.getQueryParameterString());
        }
        if(url.equals("/images/create")){
            System.out.println(ip+":请求AI画图");
            HashMap<String,String> he = new HashMap<>();
            he.put("sec-fetch-site","same-origin");
            he.put("referer","https://www.bing.com/search?q=bingAI");
            Response re =  goUrl(session,"https://www.bing.com/images/create?"+session.getQueryParameterString(),he);
            re.setMimeType("text/html");
            return re;
        }
        if(url.startsWith("/images/create/async/results")){
            System.out.println(ip+":请求AI画图图片");
            String gogoUrl = url.replace("/images/create/async/results","https://www.bing.com/images/create/async/results");
            gogoUrl = gogoUrl+"?"+session.getQueryParameterString();
 //           /641f0e9c318346378e94e495ab61a703?q=a+dog&partner=sydney&showselective=1
            HashMap<String,String> he = new HashMap<>();
            he.put("sec-fetch-site","same-origin");
            he.put("referer","https://www.bing.com/images/create?partner=sydney&showselective=1&sude=1&kseed=7000");
            Response re = goUrl(session, gogoUrl,he);
            re.setMimeType("text/html");
            return re;
        }
        String r = "{\"result\":{\"value\":\"error\",\"message\":\"由于NewBing策略更新，请更新NewBingGoGo到2023.4.3版本以上。\"}}";
        return newFixedLengthResponse(Response.Status.OK,"application/json",r);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new NewBingGoGoServerWebSocket(handshake,scheduledExecutorService);
    }

    /*
     * 转发请求
     */
    public static NanoHTTPD.Response goUrl(NanoHTTPD.IHTTPSession session,String stringUrl){
        return goUrl(session,stringUrl,new HashMap<>(1));
    }


    public static NanoHTTPD.Response goUrl(NanoHTTPD.IHTTPSession session,String stringUrl,Map<String,String> addHeaders){
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            return getReturnError(e);
        }

        HttpURLConnection urlConnection;
        try{
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
           return getReturnError(e);
        }
        try {
            urlConnection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            return getReturnError(e);
        }
        urlConnection.setDoOutput(false);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(true);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setConnectTimeout(3000);

        //拷贝头信息
        Map<String,String> header = session.getHeaders();
        String[] b = {"cookie","user-agent","accept","accept-language"};
        for (String s : b) {
            String v = header.get(s);
            urlConnection.addRequestProperty(s,v);
        }
        //添加指定的头部信息
        addHeaders.forEach(urlConnection::addRequestProperty);

        //建立链接
        try {
            urlConnection.connect();
        } catch (IOException e) {
            return getReturnError(e);
        }
        int code;
        try{
            code = urlConnection.getResponseCode();
        } catch (IOException e) {
            return getReturnError(e);
        }
        //获取请求状态代码
        if(code!=200){
            urlConnection.disconnect();
            return getReturnError("此魔法链接服务器请求被bing拒绝！请稍后再试。错误代码:"+code,null,false);
        }

        //将数据全部读取然后关闭流和链接
        int len = urlConnection.getContentLength();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(len, 0));
        try(InputStream inputStream = urlConnection.getInputStream()){
            for (int i = 0; i < len; i++) {
                byteArrayOutputStream.write(inputStream.read());
            }
        }catch (FileNotFoundException e){
            urlConnection.disconnect();
            return getReturnError("此魔法链接服务器无法正常工作，请求被bing拒绝！",e,false);
        }catch (IOException e) {
            urlConnection.disconnect();
            return getReturnError(e);
        }
        urlConnection.disconnect();

        //创建用于输出的流
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return NanoHTTPD.newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                byteArrayInputStream,
                len
        );
    }

    /**
     * 获取返回的错误
     * */
    public static NanoHTTPD.Response getReturnError(Throwable error){
        return getReturnError("服务器内部发生未知错误!",error,true);
    }
    /**
     * @param all 是否全部打印
     * */
    public static NanoHTTPD.Response getReturnError(String message,Throwable error,boolean all){
        String r;
        if (error==null){
            r = "{\"result\":{\"value\":\"error\",\"message\":\""+escapeJsonString(message)+"\"}}";
        }else if(all){
            r = "{\"result\":{\"value\":\"error\",\"message\":\""+escapeJsonString(message+"详情:"+printErrorToString(error))+"\"}}";
        }else {
            r = "{\"result\":{\"value\":\"error\",\"message\":\""+escapeJsonString(message+"详情:"+error)+"\"}}";
        }
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,"application/json",r);
    }

    /**
     * 转义成json字符串
     * */
    public static String escapeJsonString(String input) {
        return input
                .replace("\\","\\\\")
                .replace("\n","\\n")
                .replace("\r","\\r")
                .replace("\t","\\t")
                .replace("\"","\\\"");
    }
    public static String printErrorToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return  sw.getBuffer().toString();
    }

}
