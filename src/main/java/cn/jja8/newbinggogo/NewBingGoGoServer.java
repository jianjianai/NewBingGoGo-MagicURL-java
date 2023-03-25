package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

public class NewBingGoGoServer extends NanoWSD {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static void main(String[] args) throws IOException {
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
        return new NewBingGoGoClientWebSocket(handshake,executor);
    }

    /*
     * 转发请求
     */
    public static NanoHTTPD.Response goUrl(NanoHTTPD.IHTTPSession session,String stringUrl){
        return goUrl(session,stringUrl,new HashMap<>(1));
    }
    public static NanoHTTPD.Response goUrl(NanoHTTPD.IHTTPSession session,String stringUrl,Map<String,String> headers){
        try {
            URL url = new URL(stringUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(true);
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setConnectTimeout(3000);

            Map<String,String> header = session.getHeaders();
            String[] b = {"cookie","user-agent","accept","accept-language"};
            for (String s : b) {
                String v = header.get(s);
                urlConnection.addRequestProperty(s,v);
            }
            headers.forEach(urlConnection::addRequestProperty);

            Response.Status status = Response.Status.lookup(urlConnection.getResponseCode());
            if(status==null){
                status =  Response.Status.INTERNAL_ERROR;
            }
            return NanoHTTPD.newFixedLengthResponse(
                    status,
                    "application/json",
                    urlConnection.getInputStream(),
                    urlConnection.getContentLengthLong()
            );
        } catch (IOException e) {
            String r = "{\"result\":{\"value\":\"error\",\"message\":\""+escapeJsonString(e.toString())+"\"}}";
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,"application/json",r);
        }
    }


    public static String escapeJsonString(String input) {
        // 创建一个StringBuilder对象，用于存储转义后的字符串
        StringBuilder output = new StringBuilder();
        // 遍历输入字符串的每个字符
        for (int i = 0; i < input.length(); i++) {
            // 获取当前字符
            char c = input.charAt(i);
            // 判断当前字符是否需要转义
            switch (c) {
                // 如果是双引号或反斜杠，添加一个反斜杠作为前缀
                case '"': case '\\':{
                    output.append('\\');
                    output.append(c);
                    break;
                }
                // 如果是换行符，添加一个反斜杠和一个n作为替代
                case '\n':{
                    output.append('\\');
                    output.append('n');
                    break;
                }
                // 如果是制表符，添加一个反斜杠和一个t作为替代
                case '\t':{
                    output.append('\\');
                    output.append('t');
                    break;
                }
                // 其他情况下，直接添加当前字符
                default :{
                    output.append(c);
                    break;
                }
            }
        }
        // 返回转义后的字符串
        return output.toString();
    }

}
