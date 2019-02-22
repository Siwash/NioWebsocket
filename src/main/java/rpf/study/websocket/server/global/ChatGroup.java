package rpf.study.websocket.server.global;

import io.netty.channel.group.ChannelGroup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatGroup {
    private static ConcurrentMap<String, ChannelGroup> chatGroupMap=new ConcurrentHashMap<>();
    public static boolean insertChatGroupMap(String name, ChannelGroup channels){
        if (name!=null&&channels!=null){
            if (!chatGroupMap.containsKey(name)){
                //防止内存溢出
                if (chatGroupMap.size()>50){
                    chatGroupMap.clear();
                }
                chatGroupMap.put(name,channels);
                return true;
            }
        }
        return false;
    }
    public static ChannelGroup getChatGroup(String name){
        return chatGroupMap.get(name);
    }
    public static boolean isExist(String name){
        return chatGroupMap.containsKey(name);
    }

}
