package rpf.study.websocket.pojo;

import java.util.List;

public class GroupChat {
    private String creator;
    private String groupName;
    private String channelId;
    private List<String> groupMembers;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public List<String> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(List<String> groupMembers) {
        this.groupMembers = groupMembers;
    }

    @Override
    public String toString() {
        return "{" +
                "creator:'" + creator + '\'' +
                ", groupName:'" + groupName + '\'' +
                ", channelId:'" + channelId + '\'' +
                ", members:" + groupMembers +
                '}';
    }
}
