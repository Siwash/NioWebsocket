package rpf.study.websocket.controller;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import rpf.study.websocket.pojo.GroupChat;
import rpf.study.websocket.pojo.PayLoad;
import rpf.study.websocket.pojo.User;
import rpf.study.websocket.server.global.ChannelSupervise;
import rpf.study.websocket.server.global.ChatGroup;
import rpf.study.websocket.server.global.OUC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {
    @RequestMapping("/")
    public String tologin(){
        return "login";
    }
    @RequestMapping("/doLogin")
    public String doLogin(String name, Map<String,Object> paramMap)
    {
        paramMap.put("name",name);
        return "chatPage";
    }
    @RequestMapping("/getOnlineUser")
    @ResponseBody
    public List<User> getOnlineUser(){
        return OUC.USER;
    }

    @RequestMapping("/createGroups")
    @ResponseBody
    public Map createGroups(@RequestBody GroupChat groupChat){
        HashMap<String, Object> map = new HashMap<>();
        if (ChatGroup.isExist(groupChat.getGroupName())){
            map.put("code","300");
            map.put("data","群聊名已存在");
            return map;
        }else{
        List<String> members = groupChat.getGroupMembers();
        DefaultChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        for (String member : members) {
            group.add(ChannelSupervise.findChannel(member));
        }
        //广播邀请信息
        members.clear();
        PayLoad payLoad = new PayLoad(PayLoad.GROUP_APPLY,groupChat.toString());
        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(payLoad.toString());
        //发送信息出去
        group.writeAndFlush(textWebSocketFrame);
        //清空
        group.clear();
        group.add(ChannelSupervise.findChannel(groupChat.getCreator()));
        //只保留创建者
        members.add(groupChat.getCreator());
        ChatGroup.insertChatGroupMap(groupChat.getGroupName(),group);
        map.put("code","200");
        map.put("data",groupChat);
        return map;
        }
    }
    @RequestMapping("/joinGroups")
    @ResponseBody
    public Map joinGroups(String channelId,String groupName)
    {
        HashMap<String, Object> map = new HashMap<>();
        if (ChatGroup.isExist(groupName)){
            map.put("code","200");
            ChannelGroup channels = ChatGroup.getChatGroup(groupName);
            channels.add(ChannelSupervise.findChannel(channelId));
            map.put("data","已成功加入");
        }else {
            map.put("code","300");
            map.put("data","邀请已失效");
        }
        return map;
    }

}
