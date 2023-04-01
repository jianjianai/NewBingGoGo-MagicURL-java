package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class NewBingGoGoServer extends NanoWSD {
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
        String url = session.getUri();
        if(url.startsWith("/ChatHub")){
            return super.serve(session);
        }
        if(url.startsWith("/Create")){//创建聊天
            return goUrl(session,"https://www.bing.com/turing/conversation/create");
        }
        if(url.startsWith("/bingcopilotwaitlist")){//加入候补
            return goUrl(session,"https://www.bing.com/msrewards/api/v1/enroll?publ=BINGIP&crea=MY00IA&pn=bingcopilotwaitlist&partnerId=BingRewards&pred=true&wtc=MktPage_MY0291");
        }
        if(url.startsWith("/AiDraw/Create")){
            HashMap<String,String> he = new HashMap<>();
            he.put("sec-fetch-site","same-origin");
            he.put("referer","https://www.bing.com/search?q=bingAI");
            Response re =  goUrl(session,"https://www.bing.com/images/create?"+session.getQueryParameterString(),he);
            re.setMimeType("text/html");
            return re;
        }
        if(url.startsWith("/images/create/async/results")){
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
        String r = "{\"result\":{\"value\":\"error\",\"message\":\"由于NewBing策略更新，请更新NewBingGoGo到2023.3.23V2版本以上。\"}}";
        return newFixedLengthResponse(Response.Status.OK,"application/json",r);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new NewBingGoGoServerWebSocket(handshake);
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
        //获取请求状态代码
        Response.Status status;
        try {
            status = Response.Status.lookup(urlConnection.getResponseCode());
        } catch (IOException e) {
            urlConnection.disconnect();
            return getReturnError(e);
        }
        if(Response.Status.TOO_MANY_REQUESTS.equals(status)){
            urlConnection.disconnect();
            return getReturnError("此魔法链接服务器请求过多，被bing拒绝！请稍后再试。 |"+status.getDescription(),null,false);
        }
        if(status==null){
            status =  Response.Status.INTERNAL_ERROR;
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
                status,
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
