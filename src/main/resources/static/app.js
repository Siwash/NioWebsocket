let address="ws://localhost/netty/websocket?"+name;
let onlineUser=[];//在线用户集合
let socket;//socket对象，用于全局激活send操作
let channelId;//当前用户通道的id
let friendMsgNum=0;//没用
let PChatRecord=[
    {channelId:"",num:0,chat:[{name:"",content:""}]}
]//缓存1v1历史消息的集合
//缓存群聊历史消息记录集合
let GChatRecord=[
    {groupName:"",num:0,chat:[{name:"",content:""}]}
]
let MSG={
    source:"",
    target:"",
    payLoad:{}
};//发送消息模板
let payLoad={
    type: "P",
    code:"200",
    data:""
}//消息实体格式
let Protocol={
    System:"SYS",
    PrivateChat:"P",
    GroupChat:"G",
    GroupApply:"APPLY"
}//type的各种枚举
let chatPannel={
    type:"P",
    channelId:""
}
$(function () {
    if(!window.WebSocket){
        window.WebSocket = window.MozWebSocket;
    }
    if(window.WebSocket){
        socket = new WebSocket(address);

        socket.onmessage = function(event){

            let obj=eval('(' + event.data + ')');
            // console.log(obj)
            if (obj.payLoad!=null||obj.payLoad!=undefined){
                if (obj.payLoad.type==Protocol.PrivateChat){
                    showGreeting(obj);
                }else if (obj.payLoad.type==Protocol.GroupChat) {
                    showGroupChat(obj);
                }
            }
            if (obj.type=="ON"){
                addUser(obj.data)
            } else if (obj.type=="OFF"){
                deleteUser(obj.data)
            }else if (obj.type=="SYS") {
                channelId=obj.data.channelId;
            }else if (obj.type==Protocol.GroupApply){
                showGroupApply(obj.data);
            }
        };

        socket.onopen = function(event){
            var ta = document.getElementById('status');
            ta.innerHTML = "打开WebSoket 服务正常，浏览器支持WebSoket!"+"\r\n";
            console.log("连接成功")
            requestUsers();
        };
        socket.onclose = function(event){
            var ta = document.getElementById('status');
            ta.innerHTML = "";
            ta.innerHTML = "已断开连接"+"\r\n";
            console.log("已断开连接");
            toastr.error("你被挤下线了");
        };

    }else{
        alert("您的浏览器不支持WebSocket协议！");
    }
})
//通过内置对象WebSocket 发送消息到服务器
function send(message){
    if(!window.WebSocket){return;}
    if(socket.readyState == WebSocket.OPEN){
        socket.send(message);
    }else{
        alert("WebSocket 连接没有建立成功！");
    }

}
//用户上下线
function deleteUser(user) {
    for (let i = 0; i < onlineUser.length; i++) {
        if (onlineUser[i].channelId==user.channelId){
            onlineUser.splice(i,1);
            showOnlineUser(onlineUser);
            if (chatPannel.channelId==user.channelId){
                $("#greetings").append("<tr><td style='text-align:center'>对方已离线</td></tr>");
                $("#PChatSend").hide();

            }
        }
    }
    toastr.info(user.name+"下线了");
}
//新增在线用户
function addUser(user) {
    onlineUser.push(user);
    showOnlineUser(onlineUser);
    toastr.info(user.name+"上线了");
}
//渲染在线用户
function showOnlineUser(users){
    var tmp="";
    var tmp2="";
    for(var index in users){
        var cname=users[index].name+"";
        if(cname!=name)
        {
            let appendDom="";
            appendDom="<li   class=\"panel-info\"><a class=\"panel-defuat\"  name=\""+users[index].channelId+"\" href=\"#\">"
                +users[index].name+"<span id=\""+users[index].channelId+"\" style='float: right' class=\"badge\">0</span>"+"</a></li>";
        for (let x in PChatRecord){
            if (PChatRecord[x].channelId==users[index].channelId) {
                appendDom="<li   class=\"panel-info\"><a class=\"panel-defuat\"  name=\""+users[index].channelId+"\" href=\"#\">"
                    +users[index].name+"<span id=\""+users[index].channelId+"\" style='float: right' class=\"badge\">"+PChatRecord[x].num+"</span>"+"</a></li>";
            }
        }
        tmp+=appendDom;
        tmp2+="<li>" +
            "<label>" +
            "      <input type=\"checkbox\"  value=\""+users[index].channelId+"\" name=\"groupMembers\">"+cname+
            "    </label>" +
            "</li>";
        }
    }
    $("div#collapseOne .nav.nav-stacked").html(tmp);
    $("#groupList").html(tmp2);
    $("div#collapseOne .panel-defuat").click(function(e){
        //debugger;
        chatMe($(this).attr("name"));
    });
    $("#logout").click(function (e) {
        if (socket!=null&&socket!=undefined&&socket.readyState==WebSocket.OPEN){
            socket.close();
            $("#logout").hide();
            showOnlineUser([]);
        }
    })
}
//拉取在线用户
function requestUsers() {
    $.ajax({
        type: "GET",//方法类型
        dataType: "json",//预期服务器返回的数据类型
        contentType : 'application/json;charset=UTF-8',
        url: basePath+"/getOnlineUser",//url
        success: function (result) {
            // console.log(result);//打印服务端返回的数据(调试用)
            if (result!=null&&result!=undefined) {
                showOnlineUser(result);
                onlineUser=result;
            }
        },
        error : function() {
            alert("异常！");
        }
    });
}
//渲染单人聊天框
function chatMe(toChannelId){
    if (toChannelId!=chatPannel.channelId){//如果聊天框还是当前的，就不用重新渲染
        $("#"+chatPannel.channelId).text("0");
        $("#singleChatDiv").show();
        $("#PChatSend").show();
        $("#groupChatDiv").hide();
        $("#chatMe").text(searchUser(toChannelId).name+":");
        $("#sendDiv").html("<a class=\"btn btn-lg btn-info\"  onclick=\"sendMessage('"+toChannelId+"')\">发送</a>");
        friendMsgNum=0;
        let Dom=""
        for (let x in PChatRecord){
            if (PChatRecord[x].channelId==toChannelId){
                let chatObj=PChatRecord[x].chat;
                PChatRecord[x].num=0;
                for (let i in chatObj){
                    if (chatObj[i].name==name){
                        Dom+="<tr><td style='text-align:right'>你说："+chatObj[i].content+ "</td></tr>"
                    } else{
                        Dom+="<tr><td>"+chatObj[i].name+"说："+chatObj[i].content+ "</td></tr>"
                    }
                }
            }
        }
        // console.log(PChatRecord);
        console.log(Dom);
        $("#"+toChannelId).text("正在聊天");
        $("#greetings").html(Dom);
        chatPannel.type=Protocol.PrivateChat;
        chatPannel.channelId=toChannelId;
    }
}
//单人消息发送
function sendMessage(destination) {
    let msgBody=JSON.parse(JSON.stringify(payLoad));
    msgBody.data=$("#sendMsg").val();
    msgBody.type=Protocol.PrivateChat;
    MSG.source=channelId;
    MSG.target=destination;
    MSG.payLoad=msgBody;
    send(JSON.stringify(MSG))
    $("#greetings").append("<tr><td style='text-align:right'>你说："+$("#sendMsg").val()+ "</td></tr>");
    for (let x in PChatRecord){
        if (PChatRecord[x].channelId==MSG.target){
            PChatRecord[x].chat.push({name:name,content:$("#sendMsg").val()});
            break;
        }else{
            if (PChatRecord.length-1==x){
                PChatRecord.push({channelId:destination,num:0,chat:[{name:name,content:$("#sendMsg").val()}]})
            }
        }
    }

    $("#sendMsg").val("");
}
//私聊操作 消息处理器，将新消息经行缓存
function showGreeting(obj) {
    if (obj.payLoad.type==Protocol.PrivateChat) {
        let chat = {
            channelId: obj.source,
            num: 1,
            chat: [{name: (searchUser(obj.source)).name, content: obj.payLoad.data}]
        }
        let i = 0;
        for (; i < PChatRecord.length; i++) {
            if (PChatRecord[i].channelId == obj.source) {
                PChatRecord[i].num++;
                PChatRecord[i].chat.push(chat.chat[0]);
                $("#" + obj.source).text(PChatRecord[i].num);
                if (chatPannel.type==Protocol.PrivateChat&&obj.source==chatPannel.channelId) {
                    PChatRecord[i].num=0;
                    $("#greetings").append("<tr><td>"+chat.chat[0].name+"说："+chat.chat[0].content+ "</td></tr>");
                    $("#" + obj.source).text("正在聊天");
                }else{
                    toastr.info(searchUser(obj.source).name+"发来了新的消息!");
                }

                break;
            }
        }
        i++;
        if (i>PChatRecord.length) {
            PChatRecord.push(chat);
            $("#" + obj.source).text(chat.num);
            toastr.info(searchUser(obj.source).name+"发来了新的消息!");
        }
    }
}
//根据channelId查找user
function searchUser(channelId) {
    for (let i = 0; i < onlineUser.length; i++) {
        if (onlineUser[i].channelId==channelId){
            return onlineUser[i];
        }
    }
    return {};
}

/**
 * 群聊
 * */
//追加群聊列表
function apendGroupList(groupName) {
    var targetDom=$("#groupListUL");

    targetDom.append("<li class=\"panel-info\"><a onclick='chatwithGroup(\""+groupName+"\")' href=\"#\">"+groupName+
        "<span id=\""+groupName+"\" style='float: right' class=\"badge\">"+0+"</span></a></li>");
}
//群邀请面板
function showGroupApply(payload) {
    if(payload!=null&&payload!=undefined){
        var domStr="<div class=\"alert alert-success alert-dismissible\" role=\"alert\">"+
            "<a class=\"btn btn-success\" onclick=groupApplyFeedback(\""+payload.groupName+"\")  style=\"float: right;\" data-dismiss=\"alert\" >确定加入</a>"+
            "<a class=\"btn btn-danger\"  style=\"float: right;\"  data-dismiss=\"alert\" >拒绝加入</a>"+
            "<strong>群聊申请:</strong> 来自"+payload.creator+"创建的【"+payload.groupName+"】群聊邀请</div>";
        $("#head").append(domStr);
    }
}
//群聊申请意见反馈
function groupApplyFeedback(groupName) {
    $.ajax({
        //几个参数需要注意一下
        type: "GET",//方法类型
        dataType: "json",//预期服务器返回的数据类型
        contentType : 'application/json;charset=UTF-8',
        url: basePath+"/joinGroups",//url
        data:{
            channelId:channelId,
            groupName:groupName
        },
        success: function (result) {
            // console.log(result);//打印服务端返回的数据(调试用)
            if (result.code=="200") {
                apendGroupList(groupName)
            }else{
                toastr.warning(result.data);
            }
        },
        error : function() {
            alert("异常！");
        }
    });
}
//群聊面板
function chatwithGroup(groupName) {
    if (chatPannel.channelId!=groupName){
        $("#"+chatPannel.channelId).text("0");
        chatPannel.type=Protocol.GroupChat;
        chatPannel.channelId=groupName;
        $("#singleChatDiv").hide();
        $("#groupChatDiv").show();
        $("#groupChatMe").text("群聊:"+groupName);
        $("#groupSendDiv").html("<a class=\"btn btn-lg btn-info\" onclick=\"sendMessage2('"+groupName+"')\">发送</a>");
        $("#"+groupName).text("正在群聊");
        //恢复历史记录
        let  dom=""
        for (let x in GChatRecord){
            if (GChatRecord[x].groupName==groupName){
                let chat=GChatRecord[x].chat;
                for (let i in chat){
                    if (chat[i].name==name){
                        dom+="<tr><td style='text-align:right'>你说："+chat[i].content+ "</td></tr>";
                    } else{
                        dom+="<tr><td >"+chat[i].name+"说："+chat[i].content+ "</td></tr>";
                    }
                }
                break;
            }
        }
        $("#groupGreetings").html(dom);
    }

}
//群发消息
function sendMessage2(destination) {
    let msgBody=JSON.parse(JSON.stringify(payLoad));
    msgBody.data=$("#sendMsg2").val();
    msgBody.type=Protocol.GroupChat;
    MSG.source=channelId;
    MSG.target=destination;
    MSG.payLoad=msgBody;
    send(JSON.stringify(MSG))
    $("#groupGreetings").append("<tr><td style='text-align:right'>你说："+$("#sendMsg2").val()+ "</td></tr>");
    let chat=[{name:name,content:msgBody.data}]
    for (let x in GChatRecord){
        if (GChatRecord[x].groupName==destination){
            GChatRecord[x].num=0;
            GChatRecord[x].chat.push(chat[0]);
            break;
        }else{
            if (x==GChatRecord.length-1){
                GChatRecord.push( {groupName:destination,num:0,chat:chat})
            }
        }
    }
    $("#sendMsg2").val("");
}
//群聊消息处理器，将消息保存到GChatRecord
function showGroupChat(obj) {
    let groupChat=obj.payLoad;
    let chat=[{name:searchUser(obj.source).name,content:groupChat.data}]
    for (let x in GChatRecord){
        if (GChatRecord[x].groupName==obj.target){
            GChatRecord[x].chat.push(chat[0]);
            GChatRecord[x].num++;
            if (chatPannel.type==Protocol.GroupChat&&chatPannel.channelId==obj.target) {
                GChatRecord[x].num=0;
                $("#groupGreetings").append("<tr><td >"+chat[0].name+"说："+chat[0].content+ "</td></tr>");
            }else{
                $("#"+obj.target).text(GChatRecord[x].num);
                toastr.info(obj.target+"中有新的群消息!");
            }
            break;
        }else{
            if (x==GChatRecord.length-1){
                GChatRecord.push({groupName:obj.target,num:1,chat:chat});
                $("#"+obj.target).text(1);
            }
        }
    } 
}
//创建群聊
function addGroup() {
    var data = {};
    var t = $('#groupChatAddForm').serializeArray();
    $.each(t, function() {
        data [this.name] = this.value;
    });
    var checkID=[];
    $("input[name='groupMembers']:checked").each(function(i){
        checkID[i] = $(this).val();
    });
    data.groupMembers=checkID;
    data.creator=channelId;
    // console.log(data);
    $.ajax({
        //几个参数需要注意一下
        type: "POST",//方法类型
        dataType: "json",//预期服务器返回的数据类型
        contentType : 'application/json;charset=UTF-8',
        url:basePath+ "/createGroups",//url
        data: JSON.stringify(data),
        success: function (result) {
            // console.log(result);//打印服务端返回的数据(调试用)
            if (result.code=="200") {
                apendGroupList(result.data.groupName)
                $('#collapseOne2').collapse('toggle');
            }else{
                toastr.warning(result.data);
            }

        },
        error : function() {
            alert("异常！");
        }
    });
}